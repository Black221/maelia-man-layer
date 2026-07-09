package sn.lhacksrt.maeliaserver.project.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Édition des informations générales du projet (page Initialisation). */
public record UpdateProjectRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2000) String description
) {}
