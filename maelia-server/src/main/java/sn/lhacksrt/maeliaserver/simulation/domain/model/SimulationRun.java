package sn.lhacksrt.maeliaserver.simulation.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Agrégat racine du contexte simulation.
 * POJO pur — aucune annotation Spring/JPA.
 */
public final class SimulationRun {

    private final UUID id;
    private final String modelPath;
    private final String experimentName;
    private final UUID projectId;
    private final UUID scenarioId;
    private RunStatus status;
    private final Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;
    private String gamaExperimentId;
    private int finalCycle;
    private String errorMessage;

    private SimulationRun(UUID id, String modelPath, String experimentName,
                          UUID projectId, UUID scenarioId, RunStatus status, Instant createdAt) {
        this.id = id;
        this.modelPath = modelPath;
        this.experimentName = experimentName;
        this.projectId = projectId;
        this.scenarioId = scenarioId;
        this.status = status;
        this.createdAt = createdAt;
    }

    /** Run de dev (M1) — sans projet ni scénario. */
    public static SimulationRun create(String modelPath, String experimentName) {
        return new SimulationRun(UUID.randomUUID(), modelPath, experimentName,
                null, null, RunStatus.EN_FILE, Instant.now());
    }

    /** Run projet (M4) — lié à un projet et un scénario. */
    public static SimulationRun createForProject(String modelPath, String experimentName,
                                                  UUID projectId, UUID scenarioId) {
        return new SimulationRun(UUID.randomUUID(), modelPath, experimentName,
                projectId, scenarioId, RunStatus.EN_FILE, Instant.now());
    }

    /** Reconstruction depuis la persistance (préserve l'UUID d'origine). */
    public static SimulationRun reconstitute(UUID id, String modelPath, String experimentName,
                                              UUID projectId, UUID scenarioId,
                                              RunStatus status, Instant createdAt,
                                              Instant startedAt, Instant finishedAt,
                                              String gamaExperimentId, int finalCycle,
                                              String errorMessage) {
        SimulationRun run = new SimulationRun(id, modelPath, experimentName,
                projectId, scenarioId, status, createdAt);
        run.startedAt = startedAt;
        run.finishedAt = finishedAt;
        run.gamaExperimentId = gamaExperimentId;
        run.finalCycle = finalCycle;
        run.errorMessage = errorMessage;
        return run;
    }

    // Transitions d'état (invariants protégés)

    public void markStarted(String gamaExperimentId) {
        this.gamaExperimentId = gamaExperimentId;
        this.status = RunStatus.EN_COURS;
        this.startedAt = Instant.now();
    }

    public void markFinished(int finalCycle) {
        this.status = RunStatus.TERMINE;
        this.finishedAt = Instant.now();
        this.finalCycle = finalCycle;
    }

    public void markFailed(String reason) {
        this.status = RunStatus.ECHEC;
        this.finishedAt = Instant.now();
        this.errorMessage = reason;
    }

    // Accesseurs

    public UUID getId()              { return id; }
    public String getModelPath()     { return modelPath; }
    public String getExperimentName(){ return experimentName; }
    public UUID getProjectId()       { return projectId; }
    public UUID getScenarioId()      { return scenarioId; }
    public RunStatus getStatus()     { return status; }
    public Instant getCreatedAt()    { return createdAt; }
    public Instant getStartedAt()    { return startedAt; }
    public Instant getFinishedAt()   { return finishedAt; }
    public String getGamaExperimentId() { return gamaExperimentId; }
    public int getFinalCycle()       { return finalCycle; }
    public String getErrorMessage()  { return errorMessage; }
}
