package sn.lhacksrt.maeliaserver.scenario.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sn.lhacksrt.maeliaserver.scenario.domain.model.Scenario;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
 */
@Component
public class GamaParameterBuilder {

    // Racine du volume GAMA partagé (montage du conteneur headless). Le modèle MAELIA est
    // sous {mount}/maelia/ et les includes matérialisés par projet sous
    // {mount}/maelia/projects/{id}/includes/. Valeur par défaut alignée sur docker-compose
    // (./gama-workspace monté sur /workspace, le WORKDIR de l'image GAMA).
    @Value("${gama.workspace-mount:/workspace}")
    private String workspaceMount = "/workspace";

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

        return toGamaParameters(merged);
    }

    /** name→value → entrées {type:<type GAMA>, name, value} de la commande « load ». */
    public static List<Map<String, Object>> toGamaParameters(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) return List.of();
        List<Map<String, Object>> params = new ArrayList<>(parameters.size());
        parameters.forEach((name, value) -> {
            if (value == null) return;
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("type", gamaType(value));
            p.put("name", name);
            p.put("value", value);
            params.add(p);
        });
        return params;
    }

    /** Type GAMA du paramètre, inféré de la valeur Java (aligné sur le prototype gama_client). */
    private static String gamaType(Object value) {
        if (value instanceof Boolean) return "bool";
        if (value instanceof Float || value instanceof Double
                || value instanceof java.math.BigDecimal) return "float";
        if (value instanceof Number) return "int";
        if (value instanceof Collection || value instanceof Object[]) return "list";
        return "string";
    }
}
