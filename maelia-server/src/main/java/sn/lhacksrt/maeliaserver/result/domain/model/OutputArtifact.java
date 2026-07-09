package sn.lhacksrt.maeliaserver.result.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Fichier de sortie brut produit par un run (snapshot, CSV, XML…).
 * Le contenu reste sur le volume gama-workspace ; on persiste seulement les métadonnées.
 */
public record OutputArtifact(
        UUID id,
        UUID runId,
        String name,
        ArtifactType type,
        String contentType,
        String relativePath,
        long sizeBytes,
        Instant createdAt
) {
    public static OutputArtifact create(UUID runId, String name, ArtifactType type,
                                        String contentType, String relativePath, long sizeBytes) {
        return new OutputArtifact(UUID.randomUUID(), runId, name, type,
                contentType, relativePath, sizeBytes, Instant.now());
    }
}
