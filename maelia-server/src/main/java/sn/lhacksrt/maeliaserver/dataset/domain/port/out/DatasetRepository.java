package sn.lhacksrt.maeliaserver.dataset.domain.port.out;

import sn.lhacksrt.maeliaserver.dataset.domain.model.Dataset;
import sn.lhacksrt.maeliaserver.dataset.domain.model.ValidationIssue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DatasetRepository {
    Dataset save(Dataset dataset);
    Optional<Dataset> findById(UUID id);
    /** Dataset unique du type (instanceKey null) — cas standard. */
    Optional<Dataset> findByProjectAndDataSpec(UUID projectId, String dataSpecId);
    /** Instance d'un type multi-instance (instanceKey = nom de fichier, ex. "2018.csv"). */
    Optional<Dataset> findByProjectAndDataSpecInstance(UUID projectId, String dataSpecId, String instanceKey);
    List<Dataset> findByProject(UUID projectId);
    long countByDataSpec(String dataSpecId);
    void saveValidationIssues(UUID datasetId, List<ValidationIssue> issues);
    List<ValidationIssue> findIssues(UUID datasetId);
}
