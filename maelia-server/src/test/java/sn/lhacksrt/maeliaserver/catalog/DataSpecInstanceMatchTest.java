package sn.lhacksrt.maeliaserver.catalog;

import org.junit.jupiter.api.Test;
import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.model.Orientation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Appariement d'un nom de fichier à un type multi-instance via fileNamePattern. */
class DataSpecInstanceMatchTest {

    private DataSpec spec(boolean multiInstance, String fileNamePattern) {
        return new DataSpec("commun.meteo.serieClimatique", "COMMUN", "modeleCommun/meteo/observee",
                "AAAA.csv", "CSV", "COLUMN_HEADER", Orientation.FIELDS_AS_COLUMNS, null, ";",
                "AUTO", true, null, "DAY", multiInstance, "1 fichier par annee",
                fileNamePattern, "IMPORT", null, "VERIFIED", "SEED", List.of(), List.of());
    }

    @Test
    void serieClimatiqueReconnaitLesFichiersAnnuels() {
        DataSpec ds = spec(true, "\\d{4}\\.csv");
        assertTrue(ds.matchesInstanceFileName("2018.csv"));
        assertTrue(ds.matchesInstanceFileName("2024.CSV"), "insensible à la casse");
        assertTrue(ds.matchesInstanceFileName(" 2018.csv "), "espaces tolérés");
        assertFalse(ds.matchesInstanceFileName("engrais.csv"));
        assertFalse(ds.matchesInstanceFileName("2018.txt"));
        assertFalse(ds.matchesInstanceFileName("x2018.csv"), "match complet, pas partiel");
    }

    @Test
    void prixVentesReconnaitLesScenarios() {
        DataSpec ds = spec(true, "prixVentes.+\\.csv");
        assertTrue(ds.matchesInstanceFileName("prixVentesBase.csv"));
        assertTrue(ds.matchesInstanceFileName("prixventes(scenario1).csv"));
        assertFalse(ds.matchesInstanceFileName("prixVentes.csv"), "il faut un identifiant de scénario");
    }

    @Test
    void sansMultiInstanceOuSansMotifPasDAppariement() {
        assertFalse(spec(false, "\\d{4}\\.csv").matchesInstanceFileName("2018.csv"));
        assertFalse(spec(true, null).matchesInstanceFileName("2018.csv"));
        assertFalse(spec(true, "  ").matchesInstanceFileName("2018.csv"));
        assertFalse(spec(true, "\\d{4}\\.csv").matchesInstanceFileName(null));
    }

    @Test
    void regexInvalideNeCassePasLAppariement() {
        assertFalse(spec(true, "[").matchesInstanceFileName("2018.csv"));
    }
}
