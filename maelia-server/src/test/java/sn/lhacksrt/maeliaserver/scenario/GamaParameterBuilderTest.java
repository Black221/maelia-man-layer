package sn.lhacksrt.maeliaserver.scenario;

import org.junit.jupiter.api.Test;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParamType;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterGroup;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterSpec;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.port.in.ParameterCatalogUseCase;
import sn.lhacksrt.maeliaserver.scenario.application.service.GamaParameterBuilder;
import sn.lhacksrt.maeliaserver.scenario.domain.model.Scenario;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Tests unitaires purs du builder générique scénario → paramètres GAMA (M8). */
class GamaParameterBuilderTest {

    private final UUID projectId = UUID.randomUUID();
    private final UUID runId = UUID.randomUUID();

    /** Catalogue de test : liste fixe de ParameterSpec (vide = tout retombe sur l'inférence). */
    private GamaParameterBuilder builderWith(ParameterSpec... specs) {
        ParameterCatalogUseCase catalog = new ParameterCatalogUseCase() {
            @Override public List<ParameterGroup> getGroups() { return List.of(); }
            @Override public List<ParameterSpec> getParameters() { return List.of(specs); }
        };
        return new GamaParameterBuilder(catalog);
    }

    private static ParameterSpec spec(String gamlName, ParamType type) {
        return new ParameterSpec(gamlName, gamlName, "general", type, null, null,
                null, null, null, null, null, null, false, 0);
    }

    private Object value(List<Map<String, Object>> params, String name) {
        return params.stream()
                .filter(p -> name.equals(p.get("name")))
                .map(p -> p.get("value"))
                .findFirst().orElse(null);
    }

    private Object type(List<Map<String, Object>> params, String name) {
        return params.stream()
                .filter(p -> name.equals(p.get("name")))
                .map(p -> p.get("type"))
                .findFirst().orElse(null);
    }

    private boolean has(List<Map<String, Object>> params, String name) {
        return params.stream().anyMatch(p -> name.equals(p.get("name")));
    }

    @Test
    void emits_overrides_and_system_managed() {
        Scenario sc = Scenario.create(projectId, "S1", null, Map.of(
                "anneeDebutSimulation", 2020,
                "nbAnneesSimulation", 5,
                "nomScenarioClimatique", "rcp8.5",
                "executerModeleHydrographique", true));

        var params = builderWith().build(sc, projectId, runId);

        // overrides du scénario
        assertEquals(2020, value(params, "anneeDebutSimulation"));
        assertEquals(5, value(params, "nbAnneesSimulation"));
        assertEquals("rcp8.5", value(params, "nomScenarioClimatique"));
        assertEquals(true, value(params, "executerModeleHydrographique"));
        // systemManaged
        assertEquals(runId.toString(), value(params, "idSimulationAPI"));
        assertEquals(false, value(params, "executerSurCluster"));
        assertTrue(value(params, "cheminModeleVersDonnees").toString().contains(projectId.toString()));
        // includes matérialisés à plat -> territoire vide forcé par le système
        assertEquals("", value(params, "nomDecoupageZonePourLectureFichiers"));
        // sans catalogue, le type est inféré de la valeur (comme le prototype gama_client)
        assertEquals("int", type(params, "anneeDebutSimulation"));
        assertEquals("bool", type(params, "executerModeleHydrographique"));
        assertEquals("string", type(params, "nomScenarioClimatique"));
        assertEquals("string", type(params, "idSimulationAPI"));
    }

    @Test
    void maps_java_values_to_gama_types() {
        Scenario sc = Scenario.create(projectId, "S4", null, Map.of(
                "unFloat", 2.5,
                "uneListe", List.of("a", "b")));
        var params = builderWith().build(sc, projectId, runId);
        assertEquals("float", type(params, "unFloat"));
        assertEquals("list", type(params, "uneListe"));
    }

    @Test
    void uses_catalog_declared_type_over_value_inference() {
        // Bug corrigé : un paramètre FLOAT dont la valeur est un entier (2) DOIT être envoyé
        // comme float, pas comme int (sinon le gama_client peut l'ignorer silencieusement).
        Scenario sc = Scenario.create(projectId, "S5", null, Map.of(
                "seuilPortance", 2,
                "idZHASimuler", "230, 540"));
        var params = builderWith(
                spec("seuilPortance", ParamType.FLOAT),
                spec("idZHASimuler", ParamType.STRING_LIST)
        ).build(sc, projectId, runId);

        assertEquals("float", type(params, "seuilPortance"), "FLOAT catalogue prime sur la valeur int");
        assertEquals(2.0, value(params, "seuilPortance"));
        assertEquals("list", type(params, "idZHASimuler"));
        assertEquals(List.of("230", "540"), value(params, "idZHASimuler"), "chaîne CSV normalisée en liste");
    }

    @Test
    void does_not_flood_defaults_when_scenario_empty() {
        Scenario sc = Scenario.create(projectId, "S2", null, Map.of());
        var params = builderWith().build(sc, projectId, runId);
        // aucun paramètre du launcher n'est envoyé par défaut, seulement le systemManaged
        assertFalse(has(params, "anneeDebutSimulation"));
        assertFalse(has(params, "executerModeleAgricole"));
        assertTrue(has(params, "idSimulationAPI"));
    }

    @Test
    void skips_null_override_values() {
        java.util.Map<String, Object> vals = new java.util.HashMap<>();
        vals.put("anneeDebutSimulation", null);
        Scenario sc = Scenario.create(projectId, "S3", null, vals);
        var params = builderWith().build(sc, projectId, runId);
        assertFalse(has(params, "anneeDebutSimulation"));
    }
}
