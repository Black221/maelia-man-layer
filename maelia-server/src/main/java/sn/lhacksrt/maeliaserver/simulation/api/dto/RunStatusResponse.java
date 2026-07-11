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
        String scenarioName,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        int finalCycle,
        String errorMessage
) {
    public static RunStatusResponse from(SimulationRun run) {
        return from(run, null);
    }

    /** Variante enrichie du nom de scénario (jointure run↔scénario côté API). */
    public static RunStatusResponse from(SimulationRun run, String scenarioName) {
        return new RunStatusResponse(
                run.getId(),
                run.getStatus(),
                run.getModelPath(),
                run.getExperimentName(),
                run.getProjectId(),
                run.getScenarioId(),
                scenarioName,
                run.getCreatedAt(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getFinalCycle(),
                run.getErrorMessage());
    }
}
