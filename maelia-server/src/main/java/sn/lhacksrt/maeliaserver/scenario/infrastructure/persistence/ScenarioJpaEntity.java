package sn.lhacksrt.maeliaserver.scenario.infrastructure.persistence;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "scenario")
public class ScenarioJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "project_id", nullable = false, columnDefinition = "uuid")
    private UUID projectId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Type(JsonType.class)
    @Column(name = "parameter_values", columnDefinition = "jsonb")
    private Map<String, Object> parameterValues;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected ScenarioJpaEntity() {}

    public ScenarioJpaEntity(UUID id, UUID projectId, String name, String description,
                             Map<String, Object> parameterValues, Instant createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.parameterValues = parameterValues;
        this.createdAt = createdAt;
    }

    public UUID getId()                              { return id; }
    public UUID getProjectId()                       { return projectId; }
    public String getName()                          { return name; }
    public void setName(String n)                    { this.name = n; }
    public String getDescription()                   { return description; }
    public void setDescription(String d)             { this.description = d; }
    public Map<String, Object> getParameterValues()  { return parameterValues; }
    public void setParameterValues(Map<String, Object> v) { this.parameterValues = v; }
    public Instant getCreatedAt()                    { return createdAt; }
    public Instant getArchivedAt()                   { return archivedAt; }
    public void setArchivedAt(Instant t)             { this.archivedAt = t; }
}
