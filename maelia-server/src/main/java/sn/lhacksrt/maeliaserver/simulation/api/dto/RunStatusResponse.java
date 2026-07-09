package sn.lhacksrt.maeliaserver.simulation.api.dto;

import sn.lhacksrt.maeliaserver.simulation.domain.model.RunStatus;
import sn.lhacksrt.maeliaserver.simulation.domain.model.SimulationRun;

import java.time.Instant;
import java.util.UUID;

public record RunStatusResponse(
        UUID id,
        RunStatus status,
        String modelPath,
        String experimentName,
        UUID projectId,
        UUID scenarioId,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        int finalCycle,
        String errorMessage
) {
    public static RunStatusResponse from(SimulationRun run) {
        return new RunStatusResponse(
                run.getId(),
                run.getStatus(),
                run.getModelPath(),
                run.getExperimentName(),
                run.getProjectId(),
                run.getScenarioId(),
                run.getCreatedAt(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getFinalCycle(),
                run.getErrorMessage());
    }
}
