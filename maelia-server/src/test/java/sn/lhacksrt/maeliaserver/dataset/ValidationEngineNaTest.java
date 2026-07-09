package sn.lhacksrt.maeliaserver.dataset;

import org.junit.jupiter.api.Test;
import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.model.FieldSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.model.Orientation;
import sn.lhacksrt.maeliaserver.dataset.application.validation.ValidationEngine;
import sn.lhacksrt.maeliaserver.dataset.domain.model.ValidationReport;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidationEngineNaTest {

    private final ValidationEngine engine = new ValidationEngine();

    private FieldSpec field(String label, String type, boolean required, List<String> allowed) {
        return new FieldSpec(null, label, null, type, null, required, null, null, null, null, allowed, 0);
    }

    private DataSpec spec(List<FieldSpec> fields) {
        return new DataSpec("t", "AGRICOLE", "f", "f.csv", "CSV", "COLUMN_HEADER",
                Orientation.FIELDS_AS_COLUMNS, null, ";", "MANUAL", true, null, "NONE",
                false, null, null, "GRID", null, "VERIFIED", "USER", List.of(), fields);
    }

    @Test
    void naIsAcceptedForNumericRequiredAndEnumFields() {
        DataSpec ds = spec(List.of(
                field("ID_ESPECE", "String", true, List.of()),
                field("RENDEMENT_MOYEN", "Double", true, List.of()),
                field("IS_HIVER", "Boolean", true, List.of("O", "N"))));

        ValidationReport r = engine.validate(ds, List.of(
                Map.of("ID_ESPECE", "NA", "RENDEMENT_MOYEN", "NA", "IS_HIVER", "[NA]")));

        assertTrue(r.valid(), "NA doit être accepté partout : " + r.issues());
        assertEquals(0, r.errorCount());
    }

    @Test
    void blankRequiredStillFails() {
        DataSpec ds = spec(List.of(field("ID_ESPECE", "String", true, List.of())));
        ValidationReport r = engine.validate(ds, List.of(Map.of("ID_ESPECE", "")));
        assertFalse(r.valid(), "Un champ requis réellement vide reste en erreur");
    }

    @Test
    void realNumberStillValidated() {
        DataSpec ds = spec(List.of(field("RENDEMENT_MOYEN", "Double", true, List.of())));
        ValidationReport ok = engine.validate(ds, List.of(Map.of("RENDEMENT_MOYEN", "12,5")));
        assertTrue(ok.valid());
        ValidationReport ko = engine.validate(ds, List.of(Map.of("RENDEMENT_MOYEN", "abc")));
        assertFalse(ko.valid());
    }
}
