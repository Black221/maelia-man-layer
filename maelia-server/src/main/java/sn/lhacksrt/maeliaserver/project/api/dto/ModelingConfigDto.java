package sn.lhacksrt.maeliaserver.project.api.dto;

import sn.lhacksrt.maeliaserver.project.domain.model.ModelingConfiguration;

import java.util.List;

public record ModelingConfigDto(
        String assolementMethod,
        String irrigationMode,
        String cropModel,
        String restrictionMethod,
        List<String> modules,
        String scenarioClimatique
) {
    public ModelingConfiguration toDomain() {
        return new ModelingConfiguration(
                assolementMethod, irrigationMode, cropModel,
                restrictionMethod, modules, scenarioClimatique
        );
    }

    public static ModelingConfigDto from(ModelingConfiguration c) {
        return new ModelingConfigDto(
                c.assolementMethod(), c.irrigationMode(), c.cropModel(),
                c.restrictionMethod(), c.modules(), c.scenarioClimatique()
        );
    }
}
