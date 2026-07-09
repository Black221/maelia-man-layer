package sn.lhacksrt.maeliaserver.paramcatalog.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "parameter_group")
public class ParameterGroupJpaEntity {

    @Id
    private String id;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "parent_id")
    private String parentId;

    protected ParameterGroupJpaEntity() {}

    public ParameterGroupJpaEntity(String id, String label, int sortOrder, String parentId) {
        this.id = id;
        this.label = label;
        this.sortOrder = sortOrder;
        this.parentId = parentId;
    }

    public String getId()       { return id; }
    public String getLabel()    { return label; }
    public int getSortOrder()   { return sortOrder; }
    public String getParentId() { return parentId; }
}
