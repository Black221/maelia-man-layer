package sn.lhacksrt.maeliaserver.preprocessing.api.dto;

import sn.lhacksrt.maeliaserver.preprocessing.domain.model.GenerationPlanEntry;

import java.util.List;

public record GenerationPlanEntryDto(
        String dataSpecId,
        String module,
        String fileName,
        String generation,
        int level,
        List<String> dependencies,
        List<String> missingDependencies,
        boolean datasetExists,
        String status
) {
    public static GenerationPlanEntryDto from(GenerationPlanEntry e) {
        return new GenerationPlanEntryDto(
                e.dataSpecId(), e.module(), e.fileName(), e.generation(), e.level(),
                e.dependencies(), e.missingDependencies(), e.datasetExists(), e.status().name());
    }
}
