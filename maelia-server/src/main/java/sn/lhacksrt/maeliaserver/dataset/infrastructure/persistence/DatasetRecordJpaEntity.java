package sn.lhacksrt.maeliaserver.dataset.infrastructure.persistence;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "dataset_record")
@Getter @Setter @NoArgsConstructor
public class DatasetRecordJpaEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    private DatasetJpaEntity dataset;

    @Column(name = "row_index", nullable = false)
    private int rowIndex;

    @Type(JsonType.class)
    @Column(name = "values", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> values;
}
