package sn.lhacksrt.maeliaserver.result.api.dto;

import java.util.List;

/** Une série = un indicateur pour une zone donnée (zone null = global). */
public record SeriesDto(
        String indicator,
        String zone,
        String unit,
        List<PointDto> points
) {
    /** Point d'une série : axe temporel (date ou cycle) + valeur. */
    public record PointDto(String date, Integer cycle, double value) {}
}
