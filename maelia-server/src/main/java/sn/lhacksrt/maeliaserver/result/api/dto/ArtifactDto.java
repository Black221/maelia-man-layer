package sn.lhacksrt.maeliaserver.result.api.dto;

import sn.lhacksrt.maeliaserver.result.domain.model.OutputArtifact;

import java.util.UUID;

public record ArtifactDto(
        UUID id,
        String name,
        String type,
        String contentType,
        long sizeBytes,
        String url
) {
    public static ArtifactDto from(OutputArtifact a) {
        return new ArtifactDto(
                a.id(), a.name(), a.type().name(), a.contentType(), a.sizeBytes(),
                "/api/v1/runs/" + a.runId() + "/artifacts/" + a.id()
        );
    }
}
