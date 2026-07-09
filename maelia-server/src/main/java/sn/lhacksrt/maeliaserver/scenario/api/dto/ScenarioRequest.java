package sn.lhacksrt.maeliaserver.scenario.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record ScenarioRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        // Valeurs de paramètres (clé = gamlName) ; seuls les écarts au défaut du catalogue.
        Map<String, Object> parameterValues
) {}
