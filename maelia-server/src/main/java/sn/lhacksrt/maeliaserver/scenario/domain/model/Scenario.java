package sn.lhacksrt.maeliaserver.scenario.domain.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Agrégat : scénario de simulation lié à un projet (M8 — piloté par le catalogue de paramètres).
 *
 * Le scénario ne porte plus de champs métiers en dur : il stocke une map de valeurs
 * {@code parameterValues} (clé = {@code gamlName} d'un ParameterSpec) ne contenant que les
 * écarts au défaut du catalogue. {@code name}/{@code description} restent des métadonnées.
 */
public final class Scenario {

    private final UUID id;
    private final UUID projectId;
    private String name;
    private String description;
    private Map<String, Object> parameterValues;
    private final Instant createdAt;
    private Instant archivedAt;

    private Scenario(UUID id, UUID projectId, String name, String description,
                     Map<String, Object> parameterValues, Instant createdAt, Instant archivedAt) {
        this.id = id;
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.parameterValues = parameterValues != null ? parameterValues : new HashMap<>();
        this.createdAt = createdAt;
        this.archivedAt = archivedAt;
    }

    public static Scenario create(UUID projectId, String name, String description,
                                  Map<String, Object> parameterValues) {
        return new Scenario(UUID.randomUUID(), projectId, name, description,
                parameterValues, Instant.now(), null);
    }

    public static Scenario reconstitute(UUID id, UUID projectId, String name, String description,
                                        Map<String, Object> parameterValues,
                                        Instant createdAt, Instant archivedAt) {
        return new Scenario(id, projectId, name, description, parameterValues, createdAt, archivedAt);
    }

    public void update(String name, String description, Map<String, Object> parameterValues) {
        this.name = name;
        this.description = description;
        this.parameterValues = parameterValues != null ? parameterValues : new HashMap<>();
    }

    public void archive() { this.archivedAt = Instant.now(); }

    public UUID getId()                             { return id; }
    public UUID getProjectId()                      { return projectId; }
    public String getName()                         { return name; }
    public String getDescription()                  { return description; }
    public Map<String, Object> getParameterValues() { return parameterValues; }
    public Instant getCreatedAt()                   { return createdAt; }
    public Instant getArchivedAt()                  { return archivedAt; }
    public boolean isArchived()                     { return archivedAt != null; }
}
