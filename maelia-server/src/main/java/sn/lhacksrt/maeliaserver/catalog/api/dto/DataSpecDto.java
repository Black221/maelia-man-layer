package sn.lhacksrt.maeliaserver.catalog.api.dto;

import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;

import java.util.List;

public record DataSpecDto(
        String id,
        String module,
        String folder,
        String fileName,
        String fileType,
        String csvFormat,
        String orientation,
        Integer matrixValueStartIndex,
        String delimiter,
        String generation,
        boolean required,
        String requiredIf,
        String temporalResolution,
        boolean multiInstance,
        String instancePattern,
        String fileNamePattern,
        String saisieMode,
        String description,
        String fieldsStatus,
        String origin,
        List<String> dependsOn,
        List<FieldSpecDto> fields
) {
    public static DataSpecDto from(DataSpec ds) {
        return new DataSpecDto(
                ds.id(), ds.module(), ds.folder(), ds.fileName(),
                ds.fileType(), ds.csvFormat(),
                ds.orientation() != null ? ds.orientation().name() : "FIELDS_AS_COLUMNS",
                ds.matrixValueStartIndex(), ds.delimiter(),
                ds.generation(),
                ds.required(), ds.requiredIf(), ds.temporalResolution(),
                ds.multiInstance(), ds.instancePattern(), ds.fileNamePattern(), ds.saisieMode(), ds.description(),
                ds.fieldsStatus(), ds.origin(), ds.dependsOn(),
                ds.fields().stream().map(FieldSpecDto::from).toList()
        );
    }
}
