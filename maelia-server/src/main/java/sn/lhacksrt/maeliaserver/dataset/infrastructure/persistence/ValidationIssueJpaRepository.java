package sn.lhacksrt.maeliaserver.dataset.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ValidationIssueJpaRepository extends JpaRepository<ValidationIssueJpaEntity, UUID> {
    List<ValidationIssueJpaEntity> findByDatasetId(UUID datasetId);

    @Modifying
    @Query("DELETE FROM ValidationIssueJpaEntity v WHERE v.datasetId = :datasetId")
    void deleteByDatasetId(UUID datasetId);
}
