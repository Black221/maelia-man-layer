package sn.lhacksrt.maeliaserver.dataset.domain.port.in;

import java.util.List;
import java.util.UUID;

/**
 * Port de lecture exposé par le contexte dataset pour les autres contextes
 * (ex. project, pour le tableau de complétude). Vue minimale, sans les enregistrements.
 */
public interface DatasetQueryPort {

    List<DatasetView> findByProject(UUID projectId);

    /** Nombre de datasets (tous projets) rattachés à un DataSpec — pour le rapport d'impact catalogue. */
    long countByDataSpec(String dataSpecId);

    record DatasetView(String dataSpecId, String status, int recordCount) {}
}
