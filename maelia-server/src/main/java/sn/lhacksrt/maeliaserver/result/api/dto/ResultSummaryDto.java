package sn.lhacksrt.maeliaserver.result.api.dto;

import java.util.List;
import java.util.UUID;

/**
 * Restitution agrégée des résultats d'un run.
 * - {@code series}  : séries brutes (par indicateur × zone, ordonnées par date/cycle)
 * - {@code yearly}  : agrégation jour → année (moyenne) pour les séries datées
 * - {@code artifacts} : fichiers de sortie (snapshots, CSV, XML)
 */
public record ResultSummaryDto(
        UUID runId,
        List<String> indicators,
        List<SeriesDto> series,
        List<SeriesDto> yearly,
        List<ArtifactDto> artifacts
) {}
