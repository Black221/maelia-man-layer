package sn.lhacksrt.maeliaserver.catalog;

import org.junit.jupiter.api.Test;
import sn.lhacksrt.maeliaserver.catalog.application.service.RequiredIfEvaluator;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Tests unitaires purs (sans Spring) de l'évaluateur requiredIf du catalogue (M6). */
class RequiredIfEvaluatorTest {

    private final RequiredIfEvaluator eval = new RequiredIfEvaluator();

    @Test
    void null_or_blank_means_always_required() {
        assertTrue(eval.isRequired(null, Map.of()));
        assertTrue(eval.isRequired("", Map.of()));
        assertTrue(eval.isRequired("   ", Map.of()));
    }

    @Test
    void module_flag_checks_modules_list() {
        Map<String, Object> withAgri = Map.of("modules", List.of("agricole", "hydrographique"));
        assertTrue(eval.isRequired("module.agricole == true", withAgri));
        assertTrue(eval.isRequired("module.hydrographique == true", withAgri));
        assertFalse(eval.isRequired("module.normatif == true", withAgri));
        assertFalse(eval.isRequired("module.agricole == true", Map.of()));
    }

    @Test
    void not_null_checks_presence_and_non_blank() {
        assertTrue(eval.isRequired("scenarioClimatique != null", Map.of("scenarioClimatique", "rcp8.5")));
        assertFalse(eval.isRequired("scenarioClimatique != null", Map.of("scenarioClimatique", "")));
        assertFalse(eval.isRequired("scenarioClimatique != null", java.util.HashMap.newHashMap(0)));
    }

    @Test
    void equality_matches_with_and_without_quotes() {
        Map<String, Object> cfg = Map.of("assolementMethod", "FONCTIONS_DE_CROYANCE", "cropModel", "AQYIELD");
        assertTrue(eval.isRequired("assolementMethod == FONCTIONS_DE_CROYANCE", cfg));
        assertTrue(eval.isRequired("cropModel == 'AQYIELD'", cfg));
        assertFalse(eval.isRequired("cropModel == 'SIMPLE'", cfg));
    }
}
