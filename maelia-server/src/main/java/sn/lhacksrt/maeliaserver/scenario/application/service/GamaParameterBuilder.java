package sn.lhacksrt.maeliaserver.scenario.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParamType;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterSpec;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.port.in.ParameterCatalogUseCase;
import sn.lhacksrt.maeliaserver.scenario.domain.model.Scenario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Construit le tableau "parameters" passé à la commande GAMA "load" (M8 — générique).
 *
 * Principe : on n'envoie QUE les écarts au défaut (les {@code parameterValues} du scénario,
 * déjà validés contre le catalogue) + les paramètres pilotés par le système (chemins, identifiants).
 * Le launcher MAELIA (launcherProjet.gaml, calqué sur le launcher de test validé) fournit
 * lui-même les valeurs par défaut des autres paramètres. Aucun paramètre du launcher n'est
 * codé en dur ici.
 *
 * Format d'une entrée : {@code {type:<type GAMA>, name:<varGaml>, value:...}} — le type est
 * le type GAMA réel (int/float/bool/string/list), comme dans le prototype headless de
 * référence (gama_client) ; PAS le littéral "parameter".
 *
 * IMPORTANT — typage : le type GAMA de chaque paramètre de scénario est déterminé par le
 * {@link ParamType} DÉCLARÉ AU CATALOGUE, pas inféré de la valeur Java. Sans cela, un paramètre
 * FLOAT dont la valeur est un entier (ex. {@code 2}, désérialisé de jsonb en Integer) était
 * envoyé comme {@code int} et pouvait être silencieusement ignoré par le gama_client. Les
 * paramètres système (hors catalogue) et les runs de dev retombent sur l'inférence par valeur.
 */
@Component
public class GamaParameterBuilder {

    // Racine du volume GAMA partagé (montage du conteneur headless). Le modèle MAELIA est
    // sous {mount}/maelia/ et les includes matérialisés par projet sous
    // {mount}/maelia/projects/{id}/includes/. Valeur par défaut alignée sur docker-compose
    // (./gama-workspace monté sur /workspace, le WORKDIR de l'image GAMA).
    @Value("${gama.workspace-mount:/workspace}")
    private String workspaceMount = "/workspace";

    private final ParameterCatalogUseCase catalog;

    public GamaParameterBuilder(ParameterCatalogUseCase catalog) {
        this.catalog = catalog;
    }

    public List<Map<String, Object>> build(Scenario scenario, UUID projectId, UUID runId) {
        Map<String, Object> merged = new LinkedHashMap<>();

        // 1) overrides du scénario (clé = gamlName)
        if (scenario != null && scenario.getParameterValues() != null) {
            scenario.getParameterValues().forEach((k, v) -> {
                if (v != null) merged.put(k, v);
            });
        }

        // 2) paramètres pilotés par le système (priment ; cf. architecture-scenario.md §4/§6)
        String mount = (workspaceMount != null ? workspaceMount : "/workspace");
        merged.put("executerSurCluster", false);
        merged.put("cheminRacineMaelia", mount + "/maelia/");
        merged.put("cheminModeleVersDonnees", mount + "/maelia/projects/" + projectId + "/includes/");
        // L'IncludesMaterializer écrit les includes À PLAT (pas de sous-dossier includes_<territoire>) :
        // le territoire doit donc être vide, sinon MAELIA cherche .../includes/<territoire>/modeleCommun/...
        merged.put("nomDecoupageZonePourLectureFichiers", "");
        merged.put("idSimulationAPI", runId.toString());
        merged.put("nomSimulation", runId.toString().substring(0, 8));

        // Types déclarés au catalogue (gamlName -> ParamType) : source de vérité du type GAMA.
        Map<String, ParamType> catalogTypes = catalog.getParameters().stream()
                .collect(Collectors.toMap(ParameterSpec::gamlName, ParameterSpec::type, (a, b) -> a));

        return toGamaParameters(merged, catalogTypes);
    }

    /**
     * name→value → entrées {type, name, value}. Le type provient du catalogue s'il est connu,
     * sinon il est inféré de la valeur (paramètres système / runs de dev).
     */
    public static List<Map<String, Object>> toGamaParameters(Map<String, Object> parameters,
                                                             Map<String, ParamType> catalogTypes) {
        if (parameters == null || parameters.isEmpty()) return List.of();
        List<Map<String, Object>> params = new ArrayList<>(parameters.size());
        parameters.forEach((name, value) -> {
            if (value == null) return;
            ParamType declared = catalogTypes == null ? null : catalogTypes.get(name);
            Map<String, Object> p = new LinkedHashMap<>();
            if (declared != null) {
                p.put("type", gamaType(declared));
                p.put("value", coerce(value, declared));
            } else {
                p.put("type", gamaType(value));
                p.put("value", value);
            }
            p.put("name", name);
            params.add(p);
        });
        // Ordre des clés purement cosmétique ; le gama_client lit par nom. On garde name en 2e
        // pour la lisibilité des logs, mais l'ordre d'insertion ci-dessus met value avant name :
        // on réinsère proprement pour {type, name, value}.
        return params.stream().map(GamaParameterBuilder::reorder).collect(Collectors.toList());
    }

    /** Variante sans catalogue (runs de dev) : typage par inférence de valeur. */
    public static List<Map<String, Object>> toGamaParameters(Map<String, Object> parameters) {
        return toGamaParameters(parameters, null);
    }

    private static Map<String, Object> reorder(Map<String, Object> p) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("type", p.get("type"));
        o.put("name", p.get("name"));
        o.put("value", p.get("value"));
        return o;
    }

    /** Type GAMA depuis le type déclaré au catalogue. */
    private static String gamaType(ParamType type) {
        return switch (type) {
            case BOOLEAN -> "bool";
            case INTEGER -> "int";
            case FLOAT -> "float";
            case STRING_LIST -> "list";
            case STRING, ENUM -> "string";
        };
    }

    /** Type GAMA du paramètre, inféré de la valeur Java (paramètres système / dev). */
    private static String gamaType(Object value) {
        if (value instanceof Boolean) return "bool";
        if (value instanceof Float || value instanceof Double
                || value instanceof java.math.BigDecimal) return "float";
        if (value instanceof Number) return "int";
        if (value instanceof Collection || value instanceof Object[]) return "list";
        return "string";
    }

    /** Coerce la valeur au type déclaré pour que la sérialisation JSON corresponde au type GAMA. */
    static Object coerce(Object value, ParamType type) {
        if (value == null) return null;
        return switch (type) {
            case BOOLEAN -> (value instanceof Boolean b) ? b : Boolean.parseBoolean(value.toString().trim());
            case INTEGER -> {
                if (value instanceof Number n) yield n.longValue();
                try { yield Long.parseLong(value.toString().trim()); }
                catch (NumberFormatException e) { yield value; }
            }
            case FLOAT -> {
                if (value instanceof Number n) yield n.doubleValue();
                try { yield Double.parseDouble(value.toString().trim().replace(',', '.')); }
                catch (NumberFormatException e) { yield value; }
            }
            case STRING_LIST -> toList(value);
            case STRING, ENUM -> value.toString();
        };
    }

    /** Normalise une valeur STRING_LIST en List<String> (accepte liste, tableau ou chaîne CSV/pipe). */
    private static List<String> toList(Object value) {
        if (value instanceof Collection<?> c) {
            return c.stream().filter(java.util.Objects::nonNull).map(Object::toString).map(String::trim)
                    .filter(s -> !s.isEmpty()).collect(Collectors.toList());
        }
        if (value instanceof Object[] arr) {
            return Arrays.stream(arr).filter(java.util.Objects::nonNull).map(Object::toString).map(String::trim)
                    .filter(s -> !s.isEmpty()).collect(Collectors.toList());
        }
        String s = value.toString().trim();
        if (s.isEmpty()) return List.of();
        return Arrays.stream(s.split("[,|]")).map(String::trim).filter(x -> !x.isEmpty())
                .collect(Collectors.toList());
    }
}
