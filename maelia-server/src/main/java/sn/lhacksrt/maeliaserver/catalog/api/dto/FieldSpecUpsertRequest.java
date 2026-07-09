package sn.lhacksrt.maeliaserver.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** Corps de création/édition d'un champ (FieldSpec). */
public record FieldSpecUpsertRequest(
        @NotBlank String label,
        Integer position,
        String infoType,
        String unit,
        Boolean required,
        String requiredIf,
        String referencesDataSpec,
        String description,
        String listSeparator,
        List<String> allowedValues
) {}
