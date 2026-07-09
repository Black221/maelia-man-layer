package sn.lhacksrt.maeliaserver.dataset.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "validation_issue")
@Getter @Setter @NoArgsConstructor
public class ValidationIssueJpaEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "dataset_id", nullable = false)
    private UUID datasetId;

    private String field;

    @Column(name = "row_index")
    private Integer rowIndex;

    @Column(nullable = false)
    private String severity;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;
}
