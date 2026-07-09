package sn.lhacksrt.maeliaserver.simulation.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SimulationRunJpaRepository extends JpaRepository<SimulationRunJpaEntity, UUID> {

    List<SimulationRunJpaEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
