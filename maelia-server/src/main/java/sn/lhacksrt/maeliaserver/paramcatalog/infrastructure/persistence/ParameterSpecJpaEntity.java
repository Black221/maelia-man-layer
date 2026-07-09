package sn.lhacksrt.maeliaserver.paramcatalog.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "parameter_spec")
public class ParameterSpecJpaEntity {

    @Id
    @Column(name = "gaml_name", length = 120)
    private String gamlName;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(name = "group_id", nullable = false, length = 80)
    private String groupId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "default_value", columnDefinition = "text")
    private String defaultValue;

    @Column(length = 50)
    private String unit;

    @Column(name = "allowed_values", columnDefinition = "text")
    private String allowedValues;   // valeurs ENUM jointes par '|'

    @Column(name = "visible_if", columnDefinition = "text")
    private String visibleIf;

    @Column(name = "enabled_if", columnDefinition = "text")
    private String enabledIf;

    @Column(name = "options_data_spec", length = 120)
    private String optionsDataSpec;

    @Column(nullable = false)
    private boolean advanced;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ParameterSpecJpaEntity() {}

    public ParameterSpecJpaEntity(String gamlName, String label, String groupId, String type,
                                  String defaultValue, String unit, String allowedValues,
                                  String visibleIf, String enabledIf, String optionsDataSpec,
                                  boolean advanced, int sortOrder) {
        this.gamlName = gamlName;
        this.label = label;
        this.groupId = groupId;
        this.type = type;
        this.defaultValue = defaultValue;
        this.unit = unit;
        this.allowedValues = allowedValues;
        this.visibleIf = visibleIf;
        this.enabledIf = enabledIf;
        this.optionsDataSpec = optionsDataSpec;
        this.advanced = advanced;
        this.sortOrder = sortOrder;
    }

    public String getGamlName()        { return gamlName; }
    public String getLabel()           { return label; }
    public String getGroupId()         { return groupId; }
    public String getType()            { return type; }
    public String getDefaultValue()    { return defaultValue; }
    public String getUnit()            { return unit; }
    public String getAllowedValues()   { return allowedValues; }
    public String getVisibleIf()       { return visibleIf; }
    public String getEnabledIf()       { return enabledIf; }
    public String getOptionsDataSpec() { return optionsDataSpec; }
    public boolean isAdvanced()        { return advanced; }
    public int getSortOrder()          { return sortOrder; }
}
