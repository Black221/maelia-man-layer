package sn.lhacksrt.maeliaserver.dataset.api.dto;

import sn.lhacksrt.maeliaserver.dataset.domain.model.DatasetFile;

import java.time.Instant;

public record DatasetFileDto(
        String fileName,
        long sizeBytes,
        Instant uploadedAt
) {
    public static DatasetFileDto from(DatasetFile f) {
        return new DatasetFileDto(f.fileName(), f.sizeBytes(), f.uploadedAt());
    }
}
