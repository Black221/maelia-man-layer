package sn.lhacksrt.maeliaserver.catalog.api.dto;

import sn.lhacksrt.maeliaserver.catalog.domain.model.FieldSpec;

import java.util.List;
import java.util.UUID;

public record FieldSpecDto(
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
        List<String> allowedValues
) {
    public static FieldSpecDto from(FieldSpec f) {
        return new FieldSpecDto(
                f.id(), f.label(), f.position(), f.infoType(), f.unit(),
                f.required(), f.requiredIf(), f.referencesDataSpec(),
                f.description(), f.listSeparator(), f.allowedValues()
        );
    }
}
