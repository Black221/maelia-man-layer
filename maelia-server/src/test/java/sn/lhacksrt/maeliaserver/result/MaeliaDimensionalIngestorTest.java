package sn.lhacksrt.maeliaserver.result;

import org.junit.jupiter.api.Test;
import sn.lhacksrt.maeliaserver.result.application.ingestion.MaeliaDimensionalIngestor;
import sn.lhacksrt.maeliaserver.result.domain.model.ResultValue;

import java.io.StringReader;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Ingestion MAELIA-aware : rendement par culture/année, unités, surfaces, préambule. */
class MaeliaDimensionalIngestorTest {

    private final MaeliaDimensionalIngestor ingestor = new MaeliaDimensionalIngestor();
    private final UUID runId = UUID.randomUUID();

    private List<ResultValue> parse(String content, String fileName) {
        return ingestor.parse(runId, new StringReader(content), fileName);
    }

    @Test
    void supports_known_files_only() {
        assertTrue(ingestor.supports("suiviOTParParcelle.csv"));
        assertTrue(ingestor.supports("SURFACEPARCELLES.CSV"));
        assertTrue(ingestor.supports("sorties_eau.csv"));
        assertFalse(ingestor.supports("autre.csv"));
        assertFalse(ingestor.supports(null));
    }

    @Test
    void extrait_rendement_par_culture_et_annee_avec_unite() {
        // En-tête réelle (unités entre crochets), 2 récoltes (blé 2020, mais 2021) + 1 ligne non-récolte.
        String csv = String.join("\n",
                "annee;date;parcelle;exploitation;culture;ITK;OT;temps[h];RECOLTE_rendement[t/ha];IRRIGATION_dose[mm]",
                "2020;200;P1;E1;ble;itk_ble;RECOLTE;2.0;6.5;",
                "2021;205;P2;E1;mais;itk_mais;RECOLTE;3.0;9.2;",
                "2021;100;P2;E1;mais;itk_mais;IRRIGATION;1.0;;30");

        List<ResultValue> values = parse(csv, "suiviOTParParcelle.csv");

        // rendement : 2 valeurs (une par récolte), avec culture + année + unité t/ha
        List<ResultValue> rdt = values.stream().filter(v -> v.indicator().equals("RECOLTE_rendement")).toList();
        assertEquals(2, rdt.size());
        ResultValue ble = rdt.stream().filter(v -> "ble".equals(v.category())).findFirst().orElseThrow();
        assertEquals(2020, ble.year());
        assertEquals(6.5, ble.value(), 1e-9);
        assertEquals("t/ha", ble.unit());
        assertEquals("P1", ble.zone());

        ResultValue mais = rdt.stream().filter(v -> "mais".equals(v.category())).findFirst().orElseThrow();
        assertEquals(2021, mais.year());
        assertEquals(9.2, mais.value(), 1e-9);

        // irrigation renseignée seulement sur la ligne IRRIGATION (rendement y est vide → non émis)
        List<ResultValue> irr = values.stream().filter(v -> v.indicator().equals("IRRIGATION_dose")).toList();
        assertEquals(1, irr.size());
        assertEquals(30.0, irr.get(0).value(), 1e-9);
        // temps[h] n'est pas dans la whitelist d'indicateurs → non émis
        assertTrue(values.stream().noneMatch(v -> v.indicator().equalsIgnoreCase("temps")));
    }

    @Test
    void extrait_surfaces_en_m2() {
        String csv = String.join("\n",
                "idParcelle;surface [m2];zone_pedo;matIrr",
                "P1;1919.94;dior;null",
                "P2;8095.70;dekk;null");
        List<ResultValue> values = parse(csv, "surfaceParcelles.csv");
        assertEquals(2, values.size());
        ResultValue p1 = values.stream().filter(v -> "P1".equals(v.zone())).findFirst().orElseThrow();
        assertEquals("surface", p1.indicator());
        assertEquals("m2", p1.unit());
        assertEquals(1919.94, p1.value(), 1e-6);
        assertNull(p1.year());
        assertNull(p1.category());
    }

    @Test
    void saute_le_preambule_detailSimulation() {
        String csv = String.join("\n",
                "PAS_HYDRO",
                "AGRI_ASSOLEMEMNT_PAR_DONNEES",
                "annee;parcelle;couvert;irrigation[mm];satisfactionHydrique[%]",
                "2020;P1;ble;120;85");
        List<ResultValue> values = parse(csv, "sorties_eau.csv");
        assertEquals(2, values.size()); // irrigation + satisfactionHydrique
        ResultValue irr = values.stream().filter(v -> v.indicator().equals("irrigation")).findFirst().orElseThrow();
        assertEquals(2020, irr.year());
        assertEquals("ble", irr.category());
        assertEquals(120.0, irr.value(), 1e-9);
    }

    @Test
    void split_label_unit() {
        assertArrayEquals(new String[]{"RECOLTE_rendement", "t/ha"},
                MaeliaDimensionalIngestor.splitLabelUnit("RECOLTE_rendement[t/ha]"));
        assertArrayEquals(new String[]{"surface", "m2"},
                MaeliaDimensionalIngestor.splitLabelUnit("surface [m2]"));
        assertArrayEquals(new String[]{"culture", null},
                MaeliaDimensionalIngestor.splitLabelUnit("culture"));
    }
}
