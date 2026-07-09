package sn.lhacksrt.maeliaserver.catalog.domain.port.in;

import sn.lhacksrt.maeliaserver.catalog.api.dto.DataSpecUpsertRequest;
import sn.lhacksrt.maeliaserver.catalog.api.dto.FieldSpecUpsertRequest;
import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;

import java.util.List;
import java.util.UUID;

/** Gestion manuelle du catalogue : CRUD des fichiers (DataSpec) et de leurs champs (FieldSpec). */
public interface CatalogAdminUseCase {

    DataSpec createDataSpec(DataSpecUpsertRequest req);

    DataSpec updateDataSpec(String id, DataSpecUpsertRequest req);

    void deleteDataSpec(String id, boolean force);

    DataSpec duplicateDataSpec(String id, String newId);

    DataSpec addField(String specId, FieldSpecUpsertRequest req);

    DataSpec updateField(String specId, UUID fieldId, FieldSpecUpsertRequest req);

    DataSpec deleteField(String specId, UUID fieldId);

    DataSpec reorderFields(String specId, List<UUID> orderedFieldIds);

    /** Rapport d'impact avant suppression/renommage. */
    Usage usage(String specId);

    record Usage(String dataSpecId, long datasetCount, List<String> referencedByFields) {}
}
