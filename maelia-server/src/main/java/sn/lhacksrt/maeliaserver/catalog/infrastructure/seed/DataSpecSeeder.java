package sn.lhacksrt.maeliaserver.catalog.infrastructure.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import sn.lhacksrt.maeliaserver.catalog.infrastructure.persistence.DataSpecJpaEntity;
import sn.lhacksrt.maeliaserver.catalog.infrastructure.persistence.DataSpecJpaRepository;
import sn.lhacksrt.maeliaserver.catalog.infrastructure.persistence.FieldSpecJpaEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component
public class DataSpecSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSpecSeeder.class);

    private final DataSpecJpaRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${maelia.catalog.seed-location:classpath:catalog/dataspec-seed-maelia.json}")
    private Resource seedResource;

    public DataSpecSeeder(DataSpecJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    // PAS de @Transactional ici : on attrape les erreurs de seed et on les logge sans faire
    // échouer le démarrage. Avec @Transactional, une erreur pendant saveAll marque la
    // transaction rollback-only et le commit relançait UnexpectedRollbackException (crash au
    // ApplicationReadyEvent, masquant la cause réelle). saveAll garde sa propre transaction.
    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (repository.count() > 0) {
            log.debug("DataSpec catalogue already loaded ({} entries), skipping seed.", repository.count());
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(seedResource.getInputStream());
            JsonNode dataSpecs = root.path("dataSpecs");

            List<DataSpecJpaEntity> entities = new ArrayList<>();
            int order = 0;

            for (JsonNode ds : dataSpecs) {
                DataSpecJpaEntity entity = new DataSpecJpaEntity();
                entity.setId(ds.path("id").asText());
                entity.setModule(ds.path("module").asText("COMMUN"));
                entity.setFolder(ds.path("folder").asText(""));
                entity.setFileName(ds.path("fileName").asText(""));
                entity.setFileType(ds.path("fileType").asText("CSV"));
                String csvFormat = nullableText(ds, "csvFormat");
                // Orientation : explicite si fournie, sinon dérivée de l'ancien csvFormat "MATRIX".
                String orientation = nullableText(ds, "orientation");
                if (orientation == null) {
                    orientation = "MATRIX".equalsIgnoreCase(csvFormat) ? "FIELDS_AS_ROWS" : "FIELDS_AS_COLUMNS";
                }
                if ("MATRIX".equalsIgnoreCase(csvFormat)) csvFormat = "COLUMN_HEADER";
                entity.setCsvFormat(csvFormat);
                entity.setOrientation(orientation);
                entity.setMatrixValueStartIndex(
                        ds.has("matrixValueStartIndex") && !ds.path("matrixValueStartIndex").isNull()
                                ? Integer.valueOf(ds.path("matrixValueStartIndex").asInt())
                                : ("FIELDS_AS_ROWS".equals(orientation) ? 1 : null));
                entity.setDelimiter(ds.path("delimiter").asText(";"));
                entity.setOrigin("SEED");
                entity.setGeneration(ds.path("generation").asText("MANUAL"));
                entity.setRequired(ds.path("required").asBoolean(true));
                entity.setRequiredIf(nullableText(ds, "requiredIf"));
                entity.setTemporalResolution(ds.path("temporalResolution").asText("NONE"));
                entity.setMultiInstance(ds.path("multiInstance").asBoolean(false));
                entity.setInstancePattern(nullableText(ds, "instancePattern"));
                entity.setFileNamePattern(nullableText(ds, "fileNamePattern"));
                entity.setSaisieMode(ds.path("saisieMode").asText("GRID"));
                entity.setDescription(nullableText(ds, "description"));
                entity.setFieldsStatus(ds.path("fieldsStatus").asText("PENDING"));
                entity.setDependsOn(joinArray(ds.path("dependsOn")));

                List<FieldSpecJpaEntity> declared = new ArrayList<>();
                for (JsonNode f : ds.path("fields")) {
                    FieldSpecJpaEntity fs = new FieldSpecJpaEntity();
                    fs.setLabel(f.path("label").asText());
                    fs.setPosition(f.has("position") && !f.path("position").isNull()
                            ? f.path("position").asInt() : null);
                    fs.setInfoType(f.path("infoType").asText("String"));
                    fs.setUnit(nullableText(f, "unit"));
                    fs.setRequired(f.path("required").asBoolean(true));
                    fs.setRequiredIf(nullableText(f, "requiredIf"));
                    fs.setReferencesDataSpec(nullableText(f, "referencesDataSpec"));
                    fs.setDescription(nullableText(f, "description"));
                    fs.setListSeparator(nullableText(f, "listSeparator"));
                    fs.setAllowedValues(nullableText(f, "allowedValues"));
                    declared.add(fs);
                }

                int fieldOrder = 0;
                for (FieldSpecJpaEntity fs : expandMatrixFields(ds, orientation, declared)) {
                    fs.setDataSpec(entity);
                    fs.setSortOrder(fieldOrder++);
                    entity.getFields().add(fs);
                }

                entities.add(entity);
                order++;
            }

            repository.saveAll(entities);
            log.info("DataSpec catalogue seeded: {} entries loaded.", order);

        } catch (Exception e) {
            log.error("Failed to seed DataSpec catalogue: {}", e.getMessage(), e);
        }
    }

    /**
     * Orientation transposée : le bloc {@code matrix.parameters} du seed porte la liste réelle
     * des lignes du fichier (ex. les 33 propriétés d'Engrais.csv). Les fields déclarés sont
     * souvent des placeholders génériques ("valeurParEngrais") insuffisants pour la grille et
     * la réécriture du CSV. On génère donc un champ par paramètre — colonne d'entête
     * ({@code parameterColumn}) en tête — en réutilisant les métadonnées déclarées quand un
     * label correspond. Sans bloc matrix, les fields déclarés sont conservés tels quels.
     */
    private List<FieldSpecJpaEntity> expandMatrixFields(JsonNode ds, String orientation,
                                                        List<FieldSpecJpaEntity> declared) {
        JsonNode matrix = ds.path("matrix");
        JsonNode parameters = matrix.path("parameters");
        if (!"FIELDS_AS_ROWS".equals(orientation) || !parameters.isArray() || parameters.isEmpty()) {
            return declared;
        }

        Map<String, FieldSpecJpaEntity> byLabel = new LinkedHashMap<>();
        for (FieldSpecJpaEntity f : declared) byLabel.putIfAbsent(f.getLabel(), f);

        // parameterColumn d'abord (ligne d'entête = nom de chaque enregistrement), puis les
        // paramètres dédupliqués (le seed contient quelques doublons, ex. bloc REPRISE_*).
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        String parameterColumn = matrix.path("parameterColumn").asText("");
        if (!parameterColumn.isBlank()) labels.add(parameterColumn.trim());
        for (JsonNode p : parameters) {
            String label = p.asText();
            if (label != null && !label.isBlank()) labels.add(label.trim());
        }

        List<FieldSpecJpaEntity> expanded = new ArrayList<>();
        for (String label : labels) {
            FieldSpecJpaEntity f = byLabel.get(label);
            if (f == null) {
                f = new FieldSpecJpaEntity();
                f.setLabel(label);
                f.setInfoType("String");
                f.setRequired(label.equals(parameterColumn));
            }
            expanded.add(f);
        }
        return expanded;
    }

    /** Tableau JSON de strings → liste '|'-séparée (format de stockage de depends_on). */
    private String joinArray(JsonNode array) {
        if (array == null || !array.isArray() || array.isEmpty()) return null;
        List<String> values = new ArrayList<>();
        for (JsonNode n : array) {
            String v = n.asText();
            if (v != null && !v.isBlank()) values.add(v.trim());
        }
        return values.isEmpty() ? null : String.join("|", values);
    }

    private String nullableText(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return (n.isNull() || n.isMissingNode() || n.asText().isBlank()) ? null : n.asText();
    }
}
