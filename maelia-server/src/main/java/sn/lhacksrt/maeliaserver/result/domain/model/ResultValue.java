package sn.lhacksrt.maeliaserver.result.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Valeur d'un indicateur extraite des sorties d'un run.
 * Une série temporelle = ensemble de ResultValue de même {indicator, zone} ordonné par date/cycle.
 * {@code zone} = null pour une valeur globale ; {@code date}/{@code cycle} portent l'axe temporel.
 */
public record ResultValue(
        UUID id,
        UUID runId,
        String indicator,
        String zone,
        LocalDate date,
        Integer cycle,
        double value,
        String unit
) {
    public static ResultValue of(UUID runId, String indicator, String zone,
                                 LocalDate date, Integer cycle, double value, String unit) {
        return new ResultValue(UUID.randomUUID(), runId, indicator, zone, date, cycle, value, unit);
    }
}
