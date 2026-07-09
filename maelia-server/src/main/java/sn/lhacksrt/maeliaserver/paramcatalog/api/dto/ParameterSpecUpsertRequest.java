package sn.lhacksrt.maeliaserver.paramcatalog.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/** Corps de création/mise à jour d'un paramètre de simulation (admin). */
public record ParameterSpecUpsertRequest(
        @NotBlank String gamlName,
        @NotBlank String label,
        @NotBlank String group,
        @NotBlank String type,
        String defaultValue,
        String unit,
        List<String> allowedValues,
        String visibleIf,
        String enabledIf,
        String optionsDataSpec,
        boolean advanced,
        int order
) {}
