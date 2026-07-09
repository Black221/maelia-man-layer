package sn.lhacksrt.maeliaserver.scenario;

import org.junit.jupiter.api.Test;
import sn.lhacksrt.maeliaserver.scenario.application.service.GamaParameterBuilder;
import sn.lhacksrt.maeliaserver.scenario.domain.model.Scenario;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Tests unitaires purs du builder générique scénario → paramètres GAMA (M8). */
class GamaParameterBuilderTest {

    private final GamaParameterBuilder builder = new GamaParameterBuilder();
    private final UUID projectId = UUID.randomUUID();
    private final UUID runId = UUID.randomUUID();

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

        var params = builder.build(sc, projectId, runId);

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
        // le type est le type GAMA réel (comme le prototype gama_client), pas "parameter"
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
        var params = builder.build(sc, projectId, runId);
        assertEquals("float", type(params, "unFloat"));
        assertEquals("list", type(params, "uneListe"));
    }

    @Test
    void does_not_flood_defaults_when_scenario_empty() {
        Scenario sc = Scenario.create(projectId, "S2", null, Map.of());
        var params = builder.build(sc, projectId, runId);
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
        var params = builder.build(sc, projectId, runId);
        assertFalse(has(params, "anneeDebutSimulation"));
    }
}
