package sn.lhacksrt.maeliaserver.scenario.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScenarioJpaRepository extends JpaRepository<ScenarioJpaEntity, UUID> {

    List<ScenarioJpaEntity> findByProjectIdAndArchivedAtIsNullOrderByCreatedAtDesc(UUID projectId);
}
