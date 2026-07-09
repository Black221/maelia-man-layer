package sn.lhacksrt.maeliaserver.project.api.dto;

import sn.lhacksrt.maeliaserver.project.domain.model.CompletionEntry;

public record CompletionEntryDto(
        String dataSpecId,
        String module,
        String fileName,
        String fileType,
        String saisieMode,
        String generation,
        boolean required,
        String description,
        boolean datasetExists,
        String datasetStatus
) {
    public static CompletionEntryDto from(CompletionEntry e) {
        return new CompletionEntryDto(
                e.dataSpecId(), e.module(), e.fileName(), e.fileType(), e.saisieMode(),
                e.generation(), e.required(), e.description(),
                e.datasetExists(), e.datasetStatus()
        );
    }
}
