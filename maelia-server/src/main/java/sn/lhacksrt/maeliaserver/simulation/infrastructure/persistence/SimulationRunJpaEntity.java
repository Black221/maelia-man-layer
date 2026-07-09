package sn.lhacksrt.maeliaserver.simulation.infrastructure.persistence;

import jakarta.persistence.*;
import sn.lhacksrt.maeliaserver.simulation.domain.model.RunStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "simulation_run")
public class SimulationRunJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "model_path", nullable = false, length = 512)
    private String modelPath;

    @Column(name = "experiment_name", nullable = false, length = 256)
    private String experimentName;

    @Column(name = "project_id", columnDefinition = "uuid")
    private UUID projectId;

    @Column(name = "scenario_id", columnDefinition = "uuid")
    private UUID scenarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RunStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "gama_experiment_id", length = 128)
    private String gamaExperimentId;

    @Column(name = "final_cycle")
    private int finalCycle;

    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    protected SimulationRunJpaEntity() {}

    public SimulationRunJpaEntity(UUID id, String modelPath, String experimentName,
                                  UUID projectId, UUID scenarioId,
                                  RunStatus status, Instant createdAt) {
        this.id = id;
        this.modelPath = modelPath;
        this.experimentName = experimentName;
        this.projectId = projectId;
        this.scenarioId = scenarioId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId()                  { return id; }
    public String getModelPath()         { return modelPath; }
    public String getExperimentName()    { return experimentName; }
    public UUID getProjectId()           { return projectId; }
    public UUID getScenarioId()          { return scenarioId; }
    public RunStatus getStatus()         { return status; }
    public void setStatus(RunStatus s)   { this.status = s; }
    public Instant getCreatedAt()        { return createdAt; }
    public Instant getStartedAt()        { return startedAt; }
    public void setStartedAt(Instant t)  { this.startedAt = t; }
    public Instant getFinishedAt()       { return finishedAt; }
    public void setFinishedAt(Instant t) { this.finishedAt = t; }
    public String getGamaExperimentId()  { return gamaExperimentId; }
    public void setGamaExperimentId(String s) { this.gamaExperimentId = s; }
    public int getFinalCycle()           { return finalCycle; }
    public void setFinalCycle(int c)     { this.finalCycle = c; }
    public String getErrorMessage()      { return errorMessage; }
    public void setErrorMessage(String s){ this.errorMessage = s; }
}
