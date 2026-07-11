package sn.lhacksrt.maeliaserver.dataset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import sn.lhacksrt.maeliaserver.dataset.application.referential.DbfReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie le mini-lecteur DBF sur les vrais fichiers du socle Ferlo (échantillon SASSEME).
 * Désactivé si le socle n'est pas présent (build hors dépôt complet).
 */
class DbfReaderTest {

    private static final Path INCLUDES =
            Path.of("..", "gama-workspace", "maelia", "includes");

    static boolean soclePresent() {
        return Files.isReadable(INCLUDES.resolve("modeleAgricole/ilots/dansZone/parcelles.dbf"));
    }

    @Test
    @EnabledIf("soclePresent")
    void reads_distinct_parcel_ids() throws Exception {
        byte[] dbf = Files.readAllBytes(INCLUDES.resolve("modeleAgricole/ilots/dansZone/parcelles.dbf"));
        List<String> ids = DbfReader.distinctColumn(dbf, "ID_PARCELL");
        assertEquals(749, ids.size(), "749 parcelles distinctes attendues");
        assertTrue(ids.contains("19_001"), "l'ID par défaut du launcher doit être proposé");
        // colonne inconnue -> liste vide (pas d'exception)
        assertTrue(DbfReader.distinctColumn(dbf, "COLONNE_ABSENTE").isEmpty());
    }

    @Test
    @EnabledIf("soclePresent")
    void reads_soil_zones_and_meteo_points() throws Exception {
        byte[] sol = Files.readAllBytes(INCLUDES.resolve("modeleCommun/typesDeSol/typeDeSolParZH.dbf"));
        List<String> zones = DbfReader.distinctColumn(sol, "ZONE_PEDO");
        assertEquals(8, zones.size());

        byte[] meteo = Files.readAllBytes(INCLUDES.resolve("modeleCommun/meteo/polygonesMeteoFrance.dbf"));
        assertEquals(List.of("101"), DbfReader.distinctColumn(meteo, "ID_PDG"));
    }
}
