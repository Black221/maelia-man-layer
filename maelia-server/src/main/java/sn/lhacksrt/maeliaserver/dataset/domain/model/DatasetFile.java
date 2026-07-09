package sn.lhacksrt.maeliaserver.dataset.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Fichier binaire uploadé par l'utilisateur pour un DataSpec de type SHP (C8).
 * Un shapefile = plusieurs DatasetFile partageant le même basename (.shp/.shx/.dbf/.prj…).
 * Les octets sont stockés dans MinIO ({@code objectKey}) ; à la matérialisation, chaque
 * fichier écrase celui du socle dans includes/{spec.folder}/{fileName}.
 */
public record DatasetFile(
        UUID id,
        UUID projectId,
        String dataSpecId,
        String fileName,
        String objectKey,
        long sizeBytes,
        String contentType,
        Instant uploadedAt
) {
    public static DatasetFile create(UUID projectId, String dataSpecId, String fileName,
                                     String objectKey, long sizeBytes, String contentType) {
        return new DatasetFile(UUID.randomUUID(), projectId, dataSpecId, fileName,
                objectKey, sizeBytes, contentType, Instant.now());
    }
}
