package sn.lhacksrt.maeliaserver.result.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResultValueJpaRepository extends JpaRepository<ResultValueJpaEntity, UUID> {
    List<ResultValueJpaEntity> findByRunIdOrderByIndicatorAscObsDateAscCycleAsc(UUID runId);
    void deleteByRunId(UUID runId);
}
