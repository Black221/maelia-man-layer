package sn.lhacksrt.maeliaserver.catalog.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "field_spec")
@Getter @Setter @NoArgsConstructor
public class FieldSpecJpaEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_spec_id", nullable = false)
    private DataSpecJpaEntity dataSpec;

    private Integer position;

    @Column(nullable = false)
    private String label;

    @Column(name = "info_type", nullable = false)
    private String infoType;

    private String unit;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "required_if")
    private String requiredIf;

    @Column(name = "references_data_spec")
    private String referencesDataSpec;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "list_separator")
    private String listSeparator;

    @Column(name = "allowed_values")
    private String allowedValues;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
