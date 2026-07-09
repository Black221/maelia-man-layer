package sn.lhacksrt.maeliaserver.catalog.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.lhacksrt.maeliaserver.catalog.api.dto.DataSpecUpsertRequest;
import sn.lhacksrt.maeliaserver.catalog.api.dto.FieldSpecUpsertRequest;
import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.model.Orientation;
import sn.lhacksrt.maeliaserver.catalog.domain.port.in.CatalogAdminUseCase;
import sn.lhacksrt.maeliaserver.catalog.domain.port.in.CatalogUseCase;
import sn.lhacksrt.maeliaserver.catalog.infrastructure.persistence.DataSpecJpaEntity;
import sn.lhacksrt.maeliaserver.catalog.infrastructure.persistence.DataSpecJpaRepository;
import sn.lhacksrt.maeliaserver.catalog.infrastructure.persistence.FieldSpecJpaEntity;
import sn.lhacksrt.maeliaserver.dataset.domain.port.in.DatasetQueryPort;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class CatalogAdminService implements CatalogAdminUseCase {

    private static final Set<String> FILE_TYPES = Set.of("CSV", "SHP");
    private static final Set<String> ORIENTATIONS = Set.of("FIELDS_AS_COLUMNS", "FIELDS_AS_ROWS");
    private static final Set<String> INFO_TYPES = Set.of("String", "Integer", "Double", "Boolean", "Date");

    private final DataSpecJpaRepository repository;
    private final CatalogUseCase catalog;
    private final DatasetQueryPort datasetQuery;

    public CatalogAdminService(DataSpecJpaRepository repository, CatalogUseCase catalog,
                               DatasetQueryPort datasetQuery) {
        this.repository = repository;
        this.catalog = catalog;
        this.datasetQuery = datasetQuery;
    }

    @Override
    public DataSpec createDataSpec(DataSpecUpsertRequest req) {
        if (req.id() == null || req.id().isBlank()) throw bad("L'identifiant du fichier est obligatoire.");
        if (repository.existsById(req.id())) throw conflict("Un fichier avec l'id '" + req.id() + "' existe déjà.");
        DataSpecJpaEntity e = new DataSpecJpaEntity();
        e.setId(req.id().trim());
        applyDataSpec(e, req);
        e.setOrigin("USER");
        if (req.fields() != null) {
            int order = 0;
            for (FieldSpecUpsertRequest f : req.fields()) {
                e.getFields().add(buildField(e, f, order++));
            }
        }
        repository.save(e);
        return reload(e.getId());
    }

    @Override
    public DataSpec updateDataSpec(String id, DataSpecUpsertRequest req) {
        DataSpecJpaEntity e = find(id);
        applyDataSpec(e, req);
        e.setOrigin("USER");
        repository.save(e);
        return reload(id);
    }

    @Override
    public void deleteDataSpec(String id, boolean force) {
        DataSpecJpaEntity e = find(id);
        if (!force) {
            Usage u = usage(id);
            if (u.datasetCount() > 0 || !u.referencedByFields().isEmpty()) {
                throw conflict("Suppression bloquée : " + u.datasetCount() + " dataset(s) et "
                        + u.referencedByFields().size() + " champ(s) référencent ce fichier. Utilisez force=true.");
            }
        }
        repository.delete(e);
    }

    @Override
    public DataSpec duplicateDataSpec(String id, String newId) {
        DataSpecJpaEntity src = find(id);
        if (newId == null || newId.isBlank()) throw bad("Nouvel identifiant obligatoire.");
        if (repository.existsById(newId)) throw conflict("Un fichier avec l'id '" + newId + "' existe déjà.");
        DataSpecJpaEntity copy = new DataSpecJpaEntity();
        copy.setId(newId.trim());
        copy.setModule(src.getModule());
        copy.setFolder(src.getFolder());
        copy.setFileName(src.getFileName());
        copy.setFileType(src.getFileType());
        copy.setCsvFormat(src.getCsvFormat());
        copy.setOrientation(src.getOrientation());
        copy.setMatrixValueStartIndex(src.getMatrixValueStartIndex());
        copy.setDelimiter(src.getDelimiter());
        copy.setGeneration(src.getGeneration());
        copy.setRequired(src.isRequired());
        copy.setRequiredIf(src.getRequiredIf());
        copy.setTemporalResolution(src.getTemporalResolution());
        copy.setMultiInstance(src.isMultiInstance());
        copy.setInstancePattern(src.getInstancePattern());
        copy.setFileNamePattern(src.getFileNamePattern());
        copy.setSaisieMode(src.getSaisieMode());
        copy.setDescription(src.getDescription());
        copy.setFieldsStatus(src.getFieldsStatus());
        copy.setDependsOn(src.getDependsOn());
        copy.setOrigin("USER");
        copy.setUpdatedAt(OffsetDateTime.now());
        int order = 0;
        for (FieldSpecJpaEntity f : src.getFields()) {
            FieldSpecJpaEntity nf = new FieldSpecJpaEntity();
            nf.setDataSpec(copy);
            nf.setLabel(f.getLabel());
            nf.setPosition(f.getPosition());
            nf.setInfoType(f.getInfoType());
            nf.setUnit(f.getUnit());
            nf.setRequired(f.isRequired());
            nf.setRequiredIf(f.getRequiredIf());
            nf.setReferencesDataSpec(f.getReferencesDataSpec());
            nf.setDescription(f.getDescription());
            nf.setListSeparator(f.getListSeparator());
            nf.setAllowedValues(f.getAllowedValues());
            nf.setSortOrder(order++);
            copy.getFields().add(nf);
        }
        repository.save(copy);
        return reload(copy.getId());
    }

    @Override
    public DataSpec addField(String specId, FieldSpecUpsertRequest req) {
        DataSpecJpaEntity e = find(specId);
        if (e.getFields().stream().anyMatch(f -> f.getLabel().equalsIgnoreCase(req.label()))) {
            throw conflict("Un champ nommé '" + req.label() + "' existe déjà dans ce fichier.");
        }
        int nextOrder = e.getFields().stream().mapToInt(FieldSpecJpaEntity::getSortOrder).max().orElse(-1) + 1;
        e.getFields().add(buildField(e, req, nextOrder));
        repository.save(e);
        return reload(specId);
    }

    @Override
    public DataSpec updateField(String specId, UUID fieldId, FieldSpecUpsertRequest req) {
        DataSpecJpaEntity e = find(specId);
        FieldSpecJpaEntity f = e.getFields().stream()
                .filter(x -> x.getId().equals(fieldId)).findFirst()
                .orElseThrow(() -> bad("Champ introuvable : " + fieldId));
        if (e.getFields().stream().anyMatch(x -> !x.getId().equals(fieldId)
                && x.getLabel().equalsIgnoreCase(req.label()))) {
            throw conflict("Un autre champ porte déjà le nom '" + req.label() + "'.");
        }
        applyField(f, req);
        repository.save(e);
        return reload(specId);
    }

    @Override
    public DataSpec deleteField(String specId, UUID fieldId) {
        DataSpecJpaEntity e = find(specId);
        boolean removed = e.getFields().removeIf(f -> f.getId().equals(fieldId));
        if (!removed) throw bad("Champ introuvable : " + fieldId);
        repository.save(e);
        return reload(specId);
    }

    @Override
    public DataSpec reorderFields(String specId, List<UUID> orderedFieldIds) {
        DataSpecJpaEntity e = find(specId);
        for (int i = 0; i < orderedFieldIds.size(); i++) {
            UUID id = orderedFieldIds.get(i);
            int order = i;
            e.getFields().stream().filter(f -> f.getId().equals(id)).findFirst()
                    .ifPresent(f -> f.setSortOrder(order));
        }
        repository.save(e);
        return reload(specId);
    }

    @Override
    @Transactional(readOnly = true)
    public Usage usage(String specId) {
        long datasetCount = datasetQuery.countByDataSpec(specId);
        List<String> refs = repository.findAll().stream()
                .filter(ds -> ds.getFields().stream()
                        .anyMatch(f -> specId.equals(f.getReferencesDataSpec())))
                .map(DataSpecJpaEntity::getId)
                .toList();
        return new Usage(specId, datasetCount, refs);
    }

    /* ============================== Helpers ============================== */

    private void applyDataSpec(DataSpecJpaEntity e, DataSpecUpsertRequest req) {
        e.setModule(req.module() != null ? req.module() : "COMMUN");
        e.setFolder(req.folder() != null ? req.folder() : "");
        e.setFileName(req.fileName());
        String fileType = req.fileType() != null ? req.fileType() : "CSV";
        if (!FILE_TYPES.contains(fileType)) throw bad("fileType invalide : " + fileType);
        e.setFileType(fileType);
        e.setCsvFormat(req.csvFormat() != null ? req.csvFormat() : "COLUMN_HEADER");
        String orientation = Orientation.fromString(req.orientation()).name();
        if (!ORIENTATIONS.contains(orientation)) throw bad("orientation invalide : " + orientation);
        e.setOrientation(orientation);
        e.setMatrixValueStartIndex("FIELDS_AS_ROWS".equals(orientation)
                ? (req.matrixValueStartIndex() != null && req.matrixValueStartIndex() >= 1 ? req.matrixValueStartIndex() : 1)
                : req.matrixValueStartIndex());
        e.setDelimiter(req.delimiter() != null && !req.delimiter().isEmpty() ? req.delimiter().substring(0, 1) : ";");
        e.setGeneration(req.generation() != null ? req.generation() : "MANUAL");
        e.setRequired(req.required() == null || req.required());
        e.setRequiredIf(blankToNull(req.requiredIf()));
        e.setTemporalResolution(req.temporalResolution() != null ? req.temporalResolution() : "NONE");
        e.setMultiInstance(Boolean.TRUE.equals(req.multiInstance()));
        e.setInstancePattern(blankToNull(req.instancePattern()));
        e.setFileNamePattern(validPattern(blankToNull(req.fileNamePattern())));
        e.setSaisieMode(req.saisieMode() != null ? req.saisieMode() : "GRID");
        e.setDescription(blankToNull(req.description()));
        e.setFieldsStatus(req.fieldsStatus() != null ? req.fieldsStatus() : "PENDING");
        e.setDependsOn(req.dependsOn() != null && !req.dependsOn().isEmpty()
                ? String.join("|", req.dependsOn()) : null);
        e.setUpdatedAt(OffsetDateTime.now());
    }

    private FieldSpecJpaEntity buildField(DataSpecJpaEntity owner, FieldSpecUpsertRequest req, int order) {
        FieldSpecJpaEntity f = new FieldSpecJpaEntity();
        f.setDataSpec(owner);
        f.setSortOrder(order);
        applyField(f, req);
        return f;
    }

    private void applyField(FieldSpecJpaEntity f, FieldSpecUpsertRequest req) {
        if (req.label() == null || req.label().isBlank()) throw bad("Le label du champ est obligatoire.");
        String infoType = req.infoType() != null ? req.infoType() : "String";
        if (!INFO_TYPES.contains(infoType)) throw bad("infoType invalide : " + infoType);
        f.setLabel(req.label().trim());
        f.setPosition(req.position());
        f.setInfoType(infoType);
        f.setUnit(blankToNull(req.unit()));
        f.setRequired(req.required() == null || req.required());
        f.setRequiredIf(blankToNull(req.requiredIf()));
        f.setReferencesDataSpec(blankToNull(req.referencesDataSpec()));
        f.setDescription(blankToNull(req.description()));
        f.setListSeparator(blankToNull(req.listSeparator()));
        f.setAllowedValues(req.allowedValues() != null && !req.allowedValues().isEmpty()
                ? String.join("|", req.allowedValues()) : null);
    }

    private DataSpecJpaEntity find(String id) {
        return repository.findById(id).orElseThrow(() -> notFound("Fichier introuvable : " + id));
    }

    private DataSpec reload(String id) {
        return catalog.getDataSpec(id).orElseThrow(() -> notFound("Fichier introuvable : " + id));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Rejette un fileNamePattern non compilable plutôt que de le stocker inerte. */
    private static String validPattern(String pattern) {
        if (pattern == null) return null;
        try {
            java.util.regex.Pattern.compile(pattern);
            return pattern;
        } catch (java.util.regex.PatternSyntaxException e) {
            throw bad("fileNamePattern invalide (regex) : " + e.getMessage());
        }
    }

    private static RuntimeException bad(String msg) {
        return new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, msg);
    }

    private static RuntimeException conflict(String msg) {
        return new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, msg);
    }

    private static RuntimeException notFound(String msg) {
        return new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, msg);
    }
}
