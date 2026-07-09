package sn.lhacksrt.maeliaserver.dataset.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dataset")
@Getter @Setter @NoArgsConstructor
public class DatasetJpaEntity {

    // UUID assigné par le domaine (Dataset.create) — PAS @GeneratedValue (cf. ProjectJpaEntity).
    @Id
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "data_spec_id", nullable = false)
    private String dataSpecId;

    /** Nom de fichier de l'instance (types multi-instance, ex. "2018.csv") ; null = dataset unique. */
    @Column(name = "instance_key")
    private String instanceKey;

    @Column(nullable = false)
    private String status = "VIDE";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "dataset", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("rowIndex ASC")   // nom d'attribut JPA (colonne row_index) : garantit l'ordre des lignes
    private List<DatasetRecordJpaEntity> records = new ArrayList<>();
}
