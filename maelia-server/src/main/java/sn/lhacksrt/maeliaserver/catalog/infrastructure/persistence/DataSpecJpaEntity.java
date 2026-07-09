package sn.lhacksrt.maeliaserver.catalog.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "data_spec")
@Getter @Setter @NoArgsConstructor
public class DataSpecJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String module;

    @Column(nullable = false)
    private String folder;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "csv_format")
    private String csvFormat;

    @Column(nullable = false)
    private String orientation = "FIELDS_AS_COLUMNS";

    @Column(name = "matrix_value_start_index")
    private Integer matrixValueStartIndex;

    @Column(nullable = false, length = 1)
    private String delimiter = ";";

    @Column(nullable = false)
    private String origin = "SEED";

    @Column(name = "updated_at")
    private java.time.OffsetDateTime updatedAt;

    @Column(nullable = false)
    private String generation;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "required_if")
    private String requiredIf;

    @Column(name = "temporal_resolution", nullable = false)
    private String temporalResolution = "NONE";

    @Column(name = "multi_instance", nullable = false)
    private boolean multiInstance;

    @Column(name = "instance_pattern", columnDefinition = "TEXT")
    private String instancePattern;

    /** Regex (match complet, insensible à la casse) reconnaissant les instances d'un type multi-instance (ex. \d{4}\.csv pour AAAA.csv). */
    @Column(name = "file_name_pattern")
    private String fileNamePattern;

    @Column(name = "saisie_mode", nullable = false)
    private String saisieMode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "fields_status", nullable = false)
    private String fieldsStatus = "PENDING";

    /** Dépendances implicites "par construction" : ids data_spec séparés par '|'. */
    @Column(name = "depends_on", columnDefinition = "TEXT")
    private String dependsOn;

    @OneToMany(mappedBy = "dataSpec", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sort_order ASC")
    private List<FieldSpecJpaEntity> fields = new ArrayList<>();
}
