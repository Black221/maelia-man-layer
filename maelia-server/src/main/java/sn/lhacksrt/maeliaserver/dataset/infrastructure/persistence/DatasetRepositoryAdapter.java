package sn.lhacksrt.maeliaserver.dataset.infrastructure.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.lhacksrt.maeliaserver.dataset.domain.model.Dataset;
import sn.lhacksrt.maeliaserver.dataset.domain.model.DatasetStatus;
import sn.lhacksrt.maeliaserver.dataset.domain.model.ValidationIssue;
import sn.lhacksrt.maeliaserver.dataset.domain.port.out.DatasetRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class DatasetRepositoryAdapter implements DatasetRepository {

    private final DatasetJpaRepository datasetRepo;
    private final ValidationIssueJpaRepository issueRepo;

    public DatasetRepositoryAdapter(DatasetJpaRepository datasetRepo,
                                    ValidationIssueJpaRepository issueRepo) {
        this.datasetRepo = datasetRepo;
        this.issueRepo = issueRepo;
    }

    @Override
    @Transactional
    public Dataset save(Dataset dataset) {
        DatasetJpaEntity entity = datasetRepo.findById(dataset.getId())
                .orElse(new DatasetJpaEntity());

        entity.setId(dataset.getId());
        entity.setProjectId(dataset.getProjectId());
        entity.setDataSpecId(dataset.getDataSpecId());
        entity.setInstanceKey(dataset.getInstanceKey());
        entity.setStatus(dataset.getStatus().name());
        entity.setCreatedAt(dataset.getCreatedAt());
        entity.setUpdatedAt(dataset.getUpdatedAt());

        entity.getRecords().clear();
        List<Map<String, Object>> rows = dataset.getRecords();
        for (int i = 0; i < rows.size(); i++) {
            DatasetRecordJpaEntity rec = new DatasetRecordJpaEntity();
            rec.setDataset(entity);
            rec.setRowIndex(i);
            rec.setValues(rows.get(i));
            entity.getRecords().add(rec);
        }

        datasetRepo.save(entity);
        return dataset;
    }

    // Lectures : @Transactional(readOnly) pour que toDomain() hydrate la collection lazy
    // `records` dans la session, quel que soit l'appelant (LaunchRunService n'est pas transactionnel).

    @Override
    @Transactional(readOnly = true)
    public Optional<Dataset> findById(UUID id) {
        return datasetRepo.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Dataset> findByProjectAndDataSpec(UUID projectId, String dataSpecId) {
        return datasetRepo.findByProjectIdAndDataSpecIdAndInstanceKeyIsNull(projectId, dataSpecId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Dataset> findByProjectAndDataSpecInstance(UUID projectId, String dataSpecId, String instanceKey) {
        return datasetRepo.findByProjectIdAndDataSpecIdAndInstanceKeyIgnoreCase(projectId, dataSpecId, instanceKey).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Dataset> findByProject(UUID projectId) {
        return datasetRepo.findByProjectId(projectId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByDataSpec(String dataSpecId) {
        return datasetRepo.countByDataSpecId(dataSpecId);
    }

    @Override
    @Transactional
    public void saveValidationIssues(UUID datasetId, List<ValidationIssue> issues) {
        issueRepo.deleteByDatasetId(datasetId);
        issues.forEach(issue -> {
            ValidationIssueJpaEntity e = new ValidationIssueJpaEntity();
            e.setDatasetId(datasetId);
            e.setField(issue.field());
            e.setRowIndex(issue.rowIndex());
            e.setSeverity(issue.severity());
            e.setMessage(issue.message());
            issueRepo.save(e);
        });
    }

    @Override
    public List<ValidationIssue> findIssues(UUID datasetId) {
        return issueRepo.findByDatasetId(datasetId).stream()
                .map(e -> new ValidationIssue(e.getField(), e.getRowIndex(), e.getSeverity(), e.getMessage()))
                .collect(Collectors.toList());
    }

    private Dataset toDomain(DatasetJpaEntity e) {
        List<Map<String, Object>> records = e.getRecords().stream()
                .map(DatasetRecordJpaEntity::getValues)
                .collect(Collectors.toList());
        return Dataset.reconstitute(
                e.getId(), e.getProjectId(), e.getDataSpecId(), e.getInstanceKey(),
                DatasetStatus.valueOf(e.getStatus()),
                records, e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
