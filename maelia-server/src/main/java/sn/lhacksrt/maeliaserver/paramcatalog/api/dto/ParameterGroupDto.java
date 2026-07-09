package sn.lhacksrt.maeliaserver.paramcatalog.api.dto;

import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterGroup;

public record ParameterGroupDto(
        String id,
        String label,
        int order,
        String parentId
) {
    public static ParameterGroupDto from(ParameterGroup g) {
        return new ParameterGroupDto(g.id(), g.label(), g.sortOrder(), g.parentId());
    }
}
