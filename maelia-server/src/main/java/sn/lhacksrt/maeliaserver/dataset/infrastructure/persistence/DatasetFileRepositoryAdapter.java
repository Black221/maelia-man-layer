package sn.lhacksrt.maeliaserver.dataset.infrastructure.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sn.lhacksrt.maeliaserver.dataset.domain.model.DatasetFile;
import sn.lhacksrt.maeliaserver.dataset.domain.port.out.DatasetFileRepository;

import java.util.List;
import java.util.UUID;

@Component
public class DatasetFileRepositoryAdapter implements DatasetFileRepository {

    private final DatasetFileJpaRepository repo;

    public DatasetFileRepositoryAdapter(DatasetFileJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void replaceAll(UUID projectId, String dataSpecId, List<DatasetFile> files) {
        repo.deleteByProjectIdAndDataSpecId(projectId, dataSpecId);
        // Force l'exécution des DELETE avant les INSERT : Hibernate flush les insertions en
        // premier, ce qui violerait UNIQUE(project_id, data_spec_id, file_name) au ré-upload.
        repo.flush();
        repo.saveAll(files.stream().map(DatasetFileRepositoryAdapter::toEntity).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatasetFile> findByProjectAndDataSpec(UUID projectId, String dataSpecId) {
        return repo.findByProjectIdAndDataSpecId(projectId, dataSpecId).stream()
                .map(DatasetFileRepositoryAdapter::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatasetFile> findByProject(UUID projectId) {
        return repo.findByProjectId(projectId).stream()
                .map(DatasetFileRepositoryAdapter::toDomain).toList();
    }

    private static DatasetFileJpaEntity toEntity(DatasetFile f) {
        DatasetFileJpaEntity e = new DatasetFileJpaEntity();
        e.setId(f.id());
        e.setProjectId(f.projectId());
        e.setDataSpecId(f.dataSpecId());
        e.setFileName(f.fileName());
        e.setObjectKey(f.objectKey());
        e.setSizeBytes(f.sizeBytes());
        e.setContentType(f.contentType());
        e.setUploadedAt(f.uploadedAt());
        return e;
    }

    private static DatasetFile toDomain(DatasetFileJpaEntity e) {
        return new DatasetFile(e.getId(), e.getProjectId(), e.getDataSpecId(), e.getFileName(),
                e.getObjectKey(), e.getSizeBytes(), e.getContentType(), e.getUploadedAt());
    }
}
