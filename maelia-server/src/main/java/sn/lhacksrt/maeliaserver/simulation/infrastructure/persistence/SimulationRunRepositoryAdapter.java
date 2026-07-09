package sn.lhacksrt.maeliaserver.simulation.infrastructure.persistence;

import org.springframework.stereotype.Component;
import sn.lhacksrt.maeliaserver.simulation.domain.model.RunStatus;
import sn.lhacksrt.maeliaserver.simulation.domain.model.SimulationRun;
import sn.lhacksrt.maeliaserver.simulation.domain.port.out.SimulationRunRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SimulationRunRepositoryAdapter implements SimulationRunRepository {

    private final SimulationRunJpaRepository jpa;

    public SimulationRunRepositoryAdapter(SimulationRunJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public SimulationRun save(SimulationRun run) {
        SimulationRunJpaEntity entity = toEntity(run);
        jpa.save(entity);
        return run;
    }

    @Override
    public Optional<SimulationRun> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<SimulationRun> findByProject(UUID projectId) {
        return jpa.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toDomain)
                .toList();
    }

    // ------------------------------------------------------------------ //

    private SimulationRunJpaEntity toEntity(SimulationRun run) {
        SimulationRunJpaEntity entity = jpa.findById(run.getId())
                .orElseGet(() -> new SimulationRunJpaEntity(
                        run.getId(), run.getModelPath(), run.getExperimentName(),
                        run.getProjectId(), run.getScenarioId(),
                        run.getStatus(), run.getCreatedAt()));

        entity.setStatus(run.getStatus());
        entity.setStartedAt(run.getStartedAt());
        entity.setFinishedAt(run.getFinishedAt());
        entity.setGamaExperimentId(run.getGamaExperimentId());
        entity.setFinalCycle(run.getFinalCycle());
        entity.setErrorMessage(run.getErrorMessage());
        return entity;
    }

    private SimulationRun toDomain(SimulationRunJpaEntity e) {
        return SimulationRun.reconstitute(
                e.getId(), e.getModelPath(), e.getExperimentName(),
                e.getProjectId(), e.getScenarioId(),
                e.getStatus(), e.getCreatedAt(),
                e.getStartedAt(), e.getFinishedAt(),
                e.getGamaExperimentId(), e.getFinalCycle(),
                e.getErrorMessage());
    }
}
