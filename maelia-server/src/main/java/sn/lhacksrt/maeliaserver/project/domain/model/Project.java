package sn.lhacksrt.maeliaserver.project.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Project {

    private final UUID id;
    private String name;
    private String description;
    private final String studyArea;
    private ModelingConfiguration modelingConfiguration;
    private ProjectStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Project(UUID id, String name, String description, String studyArea,
                    ModelingConfiguration config, ProjectStatus status,
                    Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.studyArea = studyArea;
        this.modelingConfiguration = config;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Project create(String name, String description) {
        Instant now = Instant.now();
        return new Project(
                UUID.randomUUID(), name, description, "ferlo-sine",
                ModelingConfiguration.defaults(), ProjectStatus.ACTIF, now, now
        );
    }

    public static Project reconstitute(UUID id, String name, String description,
                                       String studyArea, ModelingConfiguration config,
                                       ProjectStatus status, Instant createdAt, Instant updatedAt) {
        return new Project(id, name, description, studyArea, config, status, createdAt, updatedAt);
    }

    public void updateName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public void updateDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public void updateModelingConfiguration(ModelingConfiguration config) {
        this.modelingConfiguration = config;
        this.updatedAt = Instant.now();
    }

    public void archive() {
        this.status = ProjectStatus.ARCHIVE;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStudyArea() { return studyArea; }
    public ModelingConfiguration getModelingConfiguration() { return modelingConfiguration; }
    public ProjectStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
