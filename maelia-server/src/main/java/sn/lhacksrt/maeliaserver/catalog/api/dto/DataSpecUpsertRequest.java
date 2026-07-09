package sn.lhacksrt.maeliaserver.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** Corps de création/édition d'un fichier du catalogue (DataSpec). */
public record DataSpecUpsertRequest(
        @NotBlank String id,
        @NotBlank String module,
        String folder,
        @NotBlank String fileName,
        String fileType,
        String csvFormat,
        String orientation,
        Integer matrixValueStartIndex,
        String delimiter,
        String generation,
        Boolean required,
        String requiredIf,
        String temporalResolution,
        Boolean multiInstance,
        String instancePattern,
        String fileNamePattern,
        String saisieMode,
        String description,
        String fieldsStatus,
        List<String> dependsOn,
        List<FieldSpecUpsertRequest> fields
) {}
