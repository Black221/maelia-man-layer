package sn.lhacksrt.maeliaserver.result.infrastructure.persistence;

import jakarta.persistence.*;
import sn.lhacksrt.maeliaserver.result.domain.model.ArtifactType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "output_artifact")
public class OutputArtifactJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "run_id", nullable = false, columnDefinition = "uuid")
    private UUID runId;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", nullable = false, length = 20)
    private ArtifactType artifactType;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "relative_path", nullable = false, length = 1024)
    private String relativePath;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OutputArtifactJpaEntity() {}

    public OutputArtifactJpaEntity(UUID id, UUID runId, String name, ArtifactType artifactType,
                                   String contentType, String relativePath, long sizeBytes, Instant createdAt) {
        this.id = id;
        this.runId = runId;
        this.name = name;
        this.artifactType = artifactType;
        this.contentType = contentType;
        this.relativePath = relativePath;
        this.sizeBytes = sizeBytes;
        this.createdAt = createdAt;
    }

    public UUID getId()            { return id; }
    public UUID getRunId()         { return runId; }
    public String getName()        { return name; }
    public ArtifactType getArtifactType() { return artifactType; }
    public String getContentType() { return contentType; }
    public String getRelativePath(){ return relativePath; }
    public long getSizeBytes()     { return sizeBytes; }
    public Instant getCreatedAt()  { return createdAt; }
}
