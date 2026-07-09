package sn.lhacksrt.maeliaserver.scenario.api.dto;

import sn.lhacksrt.maeliaserver.scenario.domain.model.Scenario;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ScenarioResponse(
        UUID id,
        UUID projectId,
        String name,
        String description,
        Map<String, Object> parameterValues,
        Instant createdAt
) {
    public static ScenarioResponse from(Scenario s) {
        return new ScenarioResponse(
                s.getId(), s.getProjectId(), s.getName(), s.getDescription(),
                s.getParameterValues(), s.getCreatedAt());
    }
}
