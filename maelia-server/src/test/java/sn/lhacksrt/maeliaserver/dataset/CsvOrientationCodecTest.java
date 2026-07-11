package sn.lhacksrt.maeliaserver.dataset;

import org.junit.jupiter.api.Test;
import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.model.FieldSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.model.Orientation;
import sn.lhacksrt.maeliaserver.dataset.application.csv.CsvOrientationCodec;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CsvOrientationCodecTest {

    private final CsvOrientationCodec codec = new CsvOrientationCodec();

    private FieldSpec field(String label, int order) {
        return new FieldSpec(null, label, null, "String", null, false, null, null, null, null, List.of(), order);
    }

    private DataSpec spec(Orientation orientation, String csvFormat, List<FieldSpec> fields) {
        return spec(orientation, csvFormat, orientation == Orientation.FIELDS_AS_ROWS ? 1 : null, fields);
    }

    private DataSpec spec(Orientation orientation, String csvFormat, Integer matrixValueStartIndex,
                          List<FieldSpec> fields) {
        return new DataSpec("t.id", "AGRICOLE", "f", "f.csv", "CSV", csvFormat,
                orientation, matrixValueStartIndex, ";",
                "MANUAL", true, null, "NONE", false, null, null, "GRID", null, "VERIFIED", "USER", List.of(), fields);
    }

    @Test
    void readsColumnHeader() throws Exception {
        DataSpec ds = spec(Orientation.FIELDS_AS_COLUMNS, "COLUMN_HEADER",
                List.of(field("ID_EXPL", 0), field("TYPE_EXPL", 1)));
        String csv = "ID_EXPL;TYPE_EXPL\nSSM1-0001;sans_UTL\nSSM1-0002;avec_UTL\n";

        List<Map<String, Object>> recs = codec.read(in(csv), ds);

        assertEquals(2, recs.size());
        assertEquals("SSM1-0001", recs.get(0).get("ID_EXPL"));
        assertEquals("avec_UTL", recs.get(1).get("TYPE_EXPL"));
    }

    @Test
    void readsTransposedMatrix() throws Exception {
        // Champs en LIGNES ; col0 = label ; col1.. = un enregistrement (ITK) chacun.
        DataSpec ds = spec(Orientation.FIELDS_AS_ROWS, "COLUMN_HEADER",
                List.of(field("NOM_ITK", 0), field("ID_ITK", 1)));
        String csv = "NOM_ITK;arachide;mil\nID_ITK;arachide_precMil;mil_precArachide\n";

        List<Map<String, Object>> recs = codec.read(in(csv), ds);

        assertEquals(2, recs.size(), "2 colonnes de données => 2 enregistrements");
        assertEquals("arachide", recs.get(0).get("NOM_ITK"));
        assertEquals("arachide_precMil", recs.get(0).get("ID_ITK"));
        assertEquals("mil", recs.get(1).get("NOM_ITK"));
        assertEquals("mil_precArachide", recs.get(1).get("ID_ITK"));
    }

    @Test
    void columnarRoundTripUsesSemicolon() throws Exception {
        DataSpec ds = spec(Orientation.FIELDS_AS_COLUMNS, "COLUMN_HEADER",
                List.of(field("A", 0), field("B", 1)));
        List<Map<String, Object>> recs = List.of(
                Map.of("A", "1", "B", "2"), Map.of("A", "3", "B", "4"));

        StringWriter w = new StringWriter();
        codec.write(w, ds, recs);
        String out = w.toString();

        assertTrue(out.contains("A;B"), "entête séparée par ';' : " + out);
        assertTrue(out.contains("1;2"));
        // relecture symétrique
        List<Map<String, Object>> back = codec.read(in(out), ds);
        assertEquals("3", back.get(1).get("A"));
    }

    @Test
    void transposedRoundTrip() throws Exception {
        DataSpec ds = spec(Orientation.FIELDS_AS_ROWS, "COLUMN_HEADER",
                List.of(field("NOM", 0), field("ID", 1)));
        List<Map<String, Object>> recs = List.of(
                Map.of("NOM", "arachide", "ID", "a1"),
                Map.of("NOM", "mil", "ID", "m1"));

        StringWriter w = new StringWriter();
        codec.write(w, ds, recs);
        String out = w.toString();

        assertTrue(out.contains("NOM;arachide;mil"), "ligne champ NOM : " + out);
        List<Map<String, Object>> back = codec.read(in(out), ds);
        assertEquals(2, back.size());
        assertEquals("m1", back.get(1).get("ID"));
    }

    @Test
    void readsTransposedWithMetaColumn() throws Exception {
        // reglesDeDecisions.csv : col 0 = label, col 1 = méta (X./[NA]), valeurs à partir de col 2.
        DataSpec ds = spec(Orientation.FIELDS_AS_ROWS, "COLUMN_HEADER", 2,
                List.of(field("NOM_ITK", 0), field("ID_ITK", 1)));
        String csv = "NOM_ITK;X.;arachide;mil\nID_ITK;[NA];arachide_precMil;mil_precArachide\n";

        List<Map<String, Object>> recs = codec.read(in(csv), ds);

        assertEquals(2, recs.size(), "la colonne méta ne doit pas devenir un enregistrement");
        assertEquals("arachide", recs.get(0).get("NOM_ITK"));
        assertEquals("mil_precArachide", recs.get(1).get("ID_ITK"));
    }

    @Test
    void transposedWithMetaColumnRoundTrip() throws Exception {
        DataSpec ds = spec(Orientation.FIELDS_AS_ROWS, "COLUMN_HEADER", 2,
                List.of(field("NOM", 0), field("ID", 1)));
        List<Map<String, Object>> recs = List.of(Map.of("NOM", "arachide", "ID", "a1"));

        StringWriter w = new StringWriter();
        codec.write(w, ds, recs);
        String out = w.toString();

        assertTrue(out.contains("NOM;;arachide"), "colonne méta réservée à l'écriture : " + out);
        List<Map<String, Object>> back = codec.read(in(out), ds);
        assertEquals(1, back.size());
        assertEquals("a1", back.get(0).get("ID"));
    }

    @Test
    void transposedWritePreservesUndeclaredRows() throws Exception {
        // Régression Engrais : le catalogue déclare NOM + C, mais les données portent une ligne
        // supplémentaire non déclarée (plan_epandage). La réécriture NE DOIT PAS la perdre
        // (sinon la matrice se décale et GAMA crashe à l'init : Index out of bounds).
        DataSpec ds = spec(Orientation.FIELDS_AS_ROWS, "COLUMN_HEADER",
                List.of(field("nom", 0), field("C", 1)));
        java.util.Map<String, Object> e1 = new java.util.LinkedHashMap<>();
        e1.put("nom", "fumier"); e1.put("C", "7.5"); e1.put("plan_epandage", "false");
        java.util.Map<String, Object> e2 = new java.util.LinkedHashMap<>();
        e2.put("nom", "urea"); e2.put("C", "1.2"); e2.put("plan_epandage", "true");

        StringWriter w = new StringWriter();
        codec.write(w, ds, List.of(e1, e2));
        String out = w.toString();

        assertTrue(out.contains("nom;fumier;urea"), "en-tête reconstruite : " + out);
        assertTrue(out.contains("plan_epandage;false;true"),
                "la ligne non déclarée doit être préservée : " + out);
        assertEquals(3, out.trim().lines().count(), "3 lignes : nom + C + plan_epandage");
    }

    private ByteArrayInputStream in(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
