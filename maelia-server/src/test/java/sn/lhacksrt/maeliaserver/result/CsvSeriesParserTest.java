package sn.lhacksrt.maeliaserver.result;

import org.junit.jupiter.api.Test;
import sn.lhacksrt.maeliaserver.result.application.ingestion.CsvSeriesParser;
import sn.lhacksrt.maeliaserver.result.domain.model.ResultValue;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Tests unitaires purs (sans Spring) du parseur CSV → valeurs d'indicateurs (M5/M6). */
class CsvSeriesParserTest {

    private final UUID runId = UUID.randomUUID();

    @Test
    void parses_dated_series_and_skips_date_column_as_indicator() {
        String csv = """
                DATE;RRmm;Tmin;Tmax
                01/01/2019;0.0;17.87;37.72
                02/01/2019;16.5;18.76;36.68
                """;

        List<ResultValue> values = CsvSeriesParser.parse(runId, csv);

        // 2 lignes × 3 indicateurs (DATE exclue)
        assertEquals(6, values.size());
        assertTrue(values.stream().noneMatch(v -> v.indicator().equalsIgnoreCase("DATE")));
        assertTrue(values.stream().allMatch(v -> v.date() != null));
        assertTrue(values.stream().allMatch(v -> v.cycle() == null));

        ResultValue first = values.get(0);
        assertEquals(LocalDate.of(2019, 1, 1), first.date());
        assertEquals("RRmm", first.indicator());
        assertEquals(0.0, first.value());
    }

    @Test
    void uses_row_index_as_cycle_when_no_date_column() {
        String csv = """
                indicateur;valeur
                10;1.5
                20;2.5
                """;
        // pas de colonne "date" → axe = cycle ; "indicateur" et "valeur" sont numériques
        List<ResultValue> values = CsvSeriesParser.parse(runId, csv);

        assertFalse(values.isEmpty());
        assertTrue(values.stream().allMatch(v -> v.date() == null));
        assertTrue(values.stream().allMatch(v -> v.cycle() != null));
        assertEquals(0, values.get(0).cycle());
    }

    @Test
    void recognizes_zone_column_and_does_not_treat_it_as_indicator() {
        String csv = """
                ID_ZH;debit
                SSM1;12.3
                SSM2;8.1
                """;
        List<ResultValue> values = CsvSeriesParser.parse(runId, csv);

        assertEquals(2, values.size());
        assertTrue(values.stream().allMatch(v -> v.indicator().equals("debit")));
        assertEquals("SSM1", values.get(0).zone());
        assertEquals("SSM2", values.get(1).zone());
    }

    @Test
    void skips_non_numeric_and_na_values() {
        String csv = """
                DATE;val;texte
                01/01/2020;NA;bonjour
                02/01/2020;3.14;monde
                """;
        List<ResultValue> values = CsvSeriesParser.parse(runId, csv);

        // seule la valeur numérique 3.14 est retenue (NA et texte ignorés)
        assertEquals(1, values.size());
        assertEquals(3.14, values.get(0).value());
        assertEquals("val", values.get(0).indicator());
    }

    @Test
    void detects_comma_delimiter() {
        String csv = "DATE,v\n01/01/2021,5\n";
        List<ResultValue> values = CsvSeriesParser.parse(runId, csv);
        assertEquals(1, values.size());
        assertEquals(5.0, values.get(0).value());
    }

    @Test
    void empty_content_returns_empty_list() {
        assertTrue(CsvSeriesParser.parse(runId, "").isEmpty());
        assertTrue(CsvSeriesParser.parse(runId, (String) null).isEmpty());
    }
}
