package sn.lhacksrt.maeliaserver.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;
import sn.lhacksrt.maeliaserver.catalog.infrastructure.persistence.DataSpecJpaEntity;
import sn.lhacksrt.maeliaserver.catalog.infrastructure.persistence.DataSpecJpaRepository;
import sn.lhacksrt.maeliaserver.catalog.infrastructure.persistence.FieldSpecJpaEntity;
import sn.lhacksrt.maeliaserver.catalog.infrastructure.seed.DataSpecSeeder;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Expansion des blocs matrix.parameters du seed en champs réels pour les fichiers
 * transposés (ex. Engrais.csv : 2 champs génériques déclarés → 1 champ par propriété).
 */
class DataSpecSeederMatrixExpansionTest {

    private static final String SEED = """
            { "dataSpecs": [ {
              "id": "agri.engrais.engrais",
              "module": "AGRICOLE",
              "folder": "modeleAgricole/Engrais",
              "fileName": "Engrais.csv",
              "fileType": "CSV",
              "csvFormat": "MATRIX",
              "fields": [
                { "label": "nom", "infoType": "String", "required": true, "description": "Propriete" },
                { "label": "valeurParEngrais", "infoType": "Double", "required": false }
              ],
              "matrix": {
                "parameterColumn": "nom",
                "parameters": ["C", "N", "C"]
              }
            }, {
              "id": "commun.meteo.serieClimatique",
              "module": "COMMUN",
              "fileName": "AAAA.csv",
              "fileType": "CSV",
              "csvFormat": "COLUMN_HEADER",
              "multiInstance": true,
              "fileNamePattern": "\\\\d{4}\\\\.csv",
              "fields": [ { "label": "date", "infoType": "Date" } ]
            } ] }
            """;

    @Test
    void expanseLesParametresMatriceEtLitLeFileNamePattern() {
        DataSpecJpaRepository repo = mock(DataSpecJpaRepository.class);
        when(repo.count()).thenReturn(0L);

        DataSpecSeeder seeder = new DataSpecSeeder(repo, new ObjectMapper());
        ReflectionTestUtils.setField(seeder, "seedResource",
                new ByteArrayResource(SEED.getBytes(StandardCharsets.UTF_8)));
        seeder.seed();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DataSpecJpaEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repo).saveAll(captor.capture());
        List<DataSpecJpaEntity> saved = captor.getValue();
        assertEquals(2, saved.size());

        DataSpecJpaEntity engrais = saved.get(0);
        assertEquals("FIELDS_AS_ROWS", engrais.getOrientation());
        assertEquals("COLUMN_HEADER", engrais.getCsvFormat(), "MATRIX est normalisé");
        assertEquals(1, engrais.getMatrixValueStartIndex());
        List<String> labels = engrais.getFields().stream().map(FieldSpecJpaEntity::getLabel).toList();
        assertEquals(List.of("nom", "C", "N"), labels,
                "parameterColumn en tête + paramètres dédupliqués ; placeholder 'valeurParEngrais' écarté");
        assertTrue(engrais.getFields().get(0).isRequired(), "métadonnées déclarées conservées pour 'nom'");
        assertEquals("Propriete", engrais.getFields().get(0).getDescription());
        assertFalse(engrais.getFields().get(1).isRequired(), "paramètre généré non requis");

        DataSpecJpaEntity meteo = saved.get(1);
        assertEquals("\\d{4}\\.csv", meteo.getFileNamePattern());
        assertEquals(List.of("date"),
                meteo.getFields().stream().map(FieldSpecJpaEntity::getLabel).toList(),
                "pas de bloc matrix : champs déclarés conservés tels quels");
    }
}
