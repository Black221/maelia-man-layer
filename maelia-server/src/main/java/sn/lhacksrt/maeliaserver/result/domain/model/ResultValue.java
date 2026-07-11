package sn.lhacksrt.maeliaserver.result.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Valeur d'un indicateur extraite des sorties d'un run.
 *
 * Dimensions :
 *  - {@code indicator} : nom de l'indicateur (ex. RECOLTE_rendement, irrigation, bilan_net_GES).
 *  - {@code category}  : dimension catégorielle agronomique (culture / couvert) ; null si sans objet.
 *  - {@code zone}      : spatialisation (parcelle / BVe) ; null = global.
 *  - {@code date}/{@code cycle}/{@code year} : axe temporel. Les sorties agronomiques MAELIA
 *    portent une {@code annee} (→ {@code year}) plutôt qu'une date journalière exploitable.
 *
 * Une série « rendement par culture au cours du temps » = ResultValue de même {indicator, category}
 * ordonnés par {@code year}.
 */
public record ResultValue(
        UUID id,
        UUID runId,
        String indicator,
        String category,
        String zone,
        LocalDate date,
        Integer cycle,
        Integer year,
        double value,
        String unit
) {
    /** Fabrique historique (générique, sans dimension catégorielle ni année). */
    public static ResultValue of(UUID runId, String indicator, String zone,
                                 LocalDate date, Integer cycle, double value, String unit) {
        return new ResultValue(UUID.randomUUID(), runId, indicator, null, zone, date, cycle, null, value, unit);
    }

    /** Fabrique dimensionnelle (ingestion MAELIA-aware : culture + année). */
    public static ResultValue of(UUID runId, String indicator, String category, String zone,
                                 LocalDate date, Integer cycle, Integer year, double value, String unit) {
        return new ResultValue(UUID.randomUUID(), runId, indicator, category, zone, date, cycle, year, value, unit);
    }
}
