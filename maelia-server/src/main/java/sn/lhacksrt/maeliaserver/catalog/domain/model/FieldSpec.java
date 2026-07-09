package sn.lhacksrt.maeliaserver.catalog.domain.model;

import java.util.List;
import java.util.UUID;

public record FieldSpec(
        UUID id,
        String label,
        Integer position,
        String infoType,
        String unit,
        boolean required,
        String requiredIf,
        String referencesDataSpec,
        String description,
        String listSeparator,
        List<String> allowedValues,
        int sortOrder
) {}
