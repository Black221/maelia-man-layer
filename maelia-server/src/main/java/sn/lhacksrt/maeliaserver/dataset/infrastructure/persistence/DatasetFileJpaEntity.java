package sn.lhacksrt.maeliaserver.dataset.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dataset_file")
@Getter @Setter @NoArgsConstructor
public class DatasetFileJpaEntity {

    // UUID assigné par le domaine (DatasetFile.create) — PAS @GeneratedValue (cf. DatasetJpaEntity).
    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "data_spec_id", nullable = false)
    private String dataSpecId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;
}
