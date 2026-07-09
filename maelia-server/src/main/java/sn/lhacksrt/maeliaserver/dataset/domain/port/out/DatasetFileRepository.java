package sn.lhacksrt.maeliaserver.dataset.domain.port.out;

import sn.lhacksrt.maeliaserver.dataset.domain.model.DatasetFile;

import java.util.List;
import java.util.UUID;

/** Métadonnées des fichiers uploadés (les octets vivent dans le stockage objet). */
public interface DatasetFileRepository {

    /** Remplace l'ensemble des fichiers de (projectId, dataSpecId) par ceux fournis. */
    void replaceAll(UUID projectId, String dataSpecId, List<DatasetFile> files);

    List<DatasetFile> findByProjectAndDataSpec(UUID projectId, String dataSpecId);

    List<DatasetFile> findByProject(UUID projectId);
}
