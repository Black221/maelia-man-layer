package sn.lhacksrt.maeliaserver.paramcatalog.api.dto;

import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParamType;
import sn.lhacksrt.maeliaserver.paramcatalog.domain.model.ParameterSpec;

import java.util.Arrays;
import java.util.List;

/** {@code defaultValue} est renvoyé typé (booléen/nombre/liste) selon {@code type}. */
public record ParameterSpecDto(
        String gamlName,
        String label,
        String group,
        String type,
        Object defaultValue,
        String unit,
        List<String> allowedValues,
        String visibleIf,
        String enabledIf,
        String optionsDataSpec,
        boolean advanced,
        int order
) {
    public static ParameterSpecDto from(ParameterSpec s) {
        return new ParameterSpecDto(
                s.gamlName(), s.label(), s.group(), s.type().name(),
                coerce(s.type(), s.defaultValue()), s.unit(), s.allowedValues(),
                s.visibleIf(), s.enabledIf(), s.optionsDataSpec(), s.advanced(), s.sortOrder());
    }

    private static Object coerce(ParamType type, String v) {
        if (v == null) return null;
        return switch (type) {
            case BOOLEAN -> Boolean.valueOf(v);
            case INTEGER -> parseLong(v);
            case FLOAT -> parseDouble(v);
            case STRING_LIST -> v.isBlank() ? List.of() : Arrays.asList(v.split("\\|", -1));
            default -> v;
        };
    }

    private static Object parseLong(String v) {
        try { return Long.valueOf(v.trim()); } catch (NumberFormatException e) { return v; }
    }

    private static Object parseDouble(String v) {
        try { return Double.valueOf(v.trim()); } catch (NumberFormatException e) { return v; }
    }
}
