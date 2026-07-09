package sn.lhacksrt.maeliaserver.dataset.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DatasetFileJpaRepository extends JpaRepository<DatasetFileJpaEntity, UUID> {

    List<DatasetFileJpaEntity> findByProjectIdAndDataSpecId(UUID projectId, String dataSpecId);

    List<DatasetFileJpaEntity> findByProjectId(UUID projectId);

    void deleteByProjectIdAndDataSpecId(UUID projectId, String dataSpecId);
}
