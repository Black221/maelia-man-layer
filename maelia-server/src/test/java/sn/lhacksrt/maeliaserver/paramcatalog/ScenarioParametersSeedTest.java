package sn.lhacksrt.maeliaserver.paramcatalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Valide l'intégrité du seed de paramètres extrait de launcherBase.gaml (M7), sans Spring. */
class ScenarioParametersSeedTest {

    private static final Set<String> TYPES =
            Set.of("BOOLEAN", "INTEGER", "FLOAT", "STRING", "ENUM", "STRING_LIST");

    @Test
    void seed_is_structurally_valid() throws Exception {
        JsonNode root;
        try (InputStream in = getClass().getResourceAsStream("/catalog/scenario-parameters-seed.json")) {
            assertNotNull(in, "seed scenario-parameters-seed.json absent du classpath");
            root = new ObjectMapper().readTree(in);
        }

        Set<String> groupIds = new HashSet<>();
        for (JsonNode g : root.path("groups")) {
            assertFalse(g.path("id").asText().isBlank(), "groupe sans id");
            groupIds.add(g.path("id").asText());
        }
        assertFalse(groupIds.isEmpty(), "aucun groupe");

        JsonNode params = root.path("parameters");
        assertTrue(params.size() > 50, "trop peu de paramètres extraits : " + params.size());

        Set<String> names = new HashSet<>();
        for (JsonNode p : params) {
            String name = p.path("gamlName").asText();
            assertFalse(name.isBlank(), "paramètre sans gamlName");
            assertTrue(names.add(name), "gamlName en double : " + name);
            assertTrue(TYPES.contains(p.path("type").asText()), "type invalide pour " + name);
            assertTrue(groupIds.contains(p.path("group").asText()), "groupe inconnu pour " + name);
            if ("ENUM".equals(p.path("type").asText())) {
                assertTrue(p.path("allowedValues").isArray() && !p.path("allowedValues").isEmpty(),
                        "ENUM sans allowedValues : " + name);
            }
        }
    }
}
