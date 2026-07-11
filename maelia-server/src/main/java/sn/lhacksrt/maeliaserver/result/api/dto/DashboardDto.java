package sn.lhacksrt.maeliaserver.result.api.dto;

import java.util.List;
import java.util.UUID;

/**
 * Tableau de bord agronomique d'un run : indicateurs disponibles + séries agrégées par
 * (indicateur × catégorie) au cours des années. Prêt à tracer côté front (une ligne/barre par
 * catégorie, X = année).
 */
public record DashboardDto(
        UUID runId,
        List<IndicatorMeta> indicators,
        List<AggSeries> series
) {
    /** Métadonnées d'un indicateur : unité + dimensions disponibles. */
    public record IndicatorMeta(String indicator, String unit,
                                List<String> categories, List<Integer> years) {}

    /** Série agrégée d'un indicateur pour une catégorie (culture/couvert), points par année. */
    public record AggSeries(String indicator, String category, String unit, List<YearPoint> points) {}

    /**
     * Point annuel : {@code mean} = moyenne de l'indicateur (ex. rendement t/ha) ;
     * {@code total} = somme pondérée par la surface (ex. production t) si l'indicateur est
     * surfacique (unité en /ha) et les surfaces connues, sinon null ; {@code count} = nb parcelles.
     */
    public record YearPoint(int year, double mean, Double total, int count) {}
}
