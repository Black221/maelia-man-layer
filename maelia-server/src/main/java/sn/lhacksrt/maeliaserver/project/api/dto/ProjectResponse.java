package sn.lhacksrt.maeliaserver.project.api.dto;

import sn.lhacksrt.maeliaserver.project.domain.model.Project;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String description,
        String studyArea,
        ModelingConfigDto modelingConfiguration,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponse from(Project p) {
        return new ProjectResponse(
                p.getId(), p.getName(), p.getDescription(), p.getStudyArea(),
                ModelingConfigDto.from(p.getModelingConfiguration()),
                p.getStatus().name(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
