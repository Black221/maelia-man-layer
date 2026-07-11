package sn.lhacksrt.maeliaserver.result.application.service;

import org.springframework.stereotype.Service;
import sn.lhacksrt.maeliaserver.result.api.dto.DashboardDto;
import sn.lhacksrt.maeliaserver.result.api.dto.DashboardDto.AggSeries;
import sn.lhacksrt.maeliaserver.result.api.dto.DashboardDto.IndicatorMeta;
import sn.lhacksrt.maeliaserver.result.api.dto.DashboardDto.YearPoint;
import sn.lhacksrt.maeliaserver.result.domain.model.ResultValue;
import sn.lhacksrt.maeliaserver.result.domain.port.out.ResultRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Agrège les valeurs d'un run en séries « indicateur × culture au cours du temps ».
 *
 * Pour un indicateur surfacique (unité en /ha, ex. rendement t/ha), la production totale par
 * (culture, année) est calculée en pondérant par la surface de chaque parcelle
 * (indicateur {@code surface} de surfaceParcelles.csv, converti en hectares).
 */
@Service
public class ResultAggregationService {

    private static final String SURFACE = "surface";
    private static final String SEP = ""; // séparateur de clé sûr (jamais dans les libellés)

    private final ResultRepository repository;

    public ResultAggregationService(ResultRepository repository) {
        this.repository = repository;
    }

    public DashboardDto dashboard(UUID runId) {
        List<ResultValue> values = repository.findValuesByRun(runId);

        // Surface (ha) par parcelle, depuis l'indicateur "surface".
        Map<String, Double> surfaceHaByZone = new LinkedHashMap<>();
        for (ResultValue v : values) {
            if (SURFACE.equalsIgnoreCase(v.indicator()) && v.zone() != null) {
                surfaceHaByZone.put(v.zone(), toHectares(v.value(), v.unit()));
            }
        }

        // Accumulateurs par clé (indicator, category, year).
        Map<String, Acc> byKey = new LinkedHashMap<>();
        Map<String, String> unitByIndicator = new LinkedHashMap<>();
        Map<String, TreeSet<String>> categoriesByIndicator = new LinkedHashMap<>();
        Map<String, TreeSet<Integer>> yearsByIndicator = new LinkedHashMap<>();

        for (ResultValue v : values) {
            if (SURFACE.equalsIgnoreCase(v.indicator())) continue; // helper, pas une série
            if (v.year() == null) continue;                        // dashboard = axe annuel
            String indicator = v.indicator();
            String category = v.category();
            int year = v.year();

            unitByIndicator.putIfAbsent(indicator, v.unit());
            if (category != null) {
                categoriesByIndicator.computeIfAbsent(indicator, k -> new TreeSet<>()).add(category);
            }
            yearsByIndicator.computeIfAbsent(indicator, k -> new TreeSet<>()).add(year);

            String key = indicator + SEP + (category == null ? "" : category) + SEP + year;
            Acc acc = byKey.computeIfAbsent(key, k -> new Acc());
            acc.sum += v.value();
            acc.count++;
            Double ha = v.zone() == null ? null : surfaceHaByZone.get(v.zone());
            if (ha != null) { acc.weighted += v.value() * ha; acc.hasSurface = true; }
        }

        // Reconstruit les séries par (indicator, category).
        Map<String, List<YearPoint>> pointsBySeries = new LinkedHashMap<>();
        Map<String, String[]> seriesMeta = new LinkedHashMap<>(); // seriesKey -> [indicator, category]
        for (Map.Entry<String, Acc> e : byKey.entrySet()) {
            String[] parts = e.getKey().split(SEP, -1);
            String indicator = parts[0];
            String category = parts[1].isEmpty() ? null : parts[1];
            int year = Integer.parseInt(parts[2]);
            Acc acc = e.getValue();
            boolean perArea = isPerArea(unitByIndicator.get(indicator));
            Double total = (perArea && acc.hasSurface) ? acc.weighted : null;
            String seriesKey = indicator + SEP + (category == null ? "" : category);
            seriesMeta.putIfAbsent(seriesKey, new String[]{indicator, category});
            pointsBySeries.computeIfAbsent(seriesKey, k -> new ArrayList<>())
                    .add(new YearPoint(year, acc.sum / acc.count, total, acc.count));
        }

        List<AggSeries> series = new ArrayList<>();
        for (Map.Entry<String, List<YearPoint>> e : pointsBySeries.entrySet()) {
            String[] meta = seriesMeta.get(e.getKey());
            List<YearPoint> pts = new ArrayList<>(e.getValue());
            pts.sort(Comparator.comparingInt(YearPoint::year));
            series.add(new AggSeries(meta[0], meta[1], unitByIndicator.get(meta[0]), pts));
        }
        series.sort(Comparator.comparing(AggSeries::indicator)
                .thenComparing(s -> s.category() == null ? "" : s.category()));

        List<IndicatorMeta> indicators = new ArrayList<>();
        for (String ind : new TreeMap<>(yearsByIndicator).keySet()) {
            indicators.add(new IndicatorMeta(ind, unitByIndicator.get(ind),
                    new ArrayList<>(categoriesByIndicator.getOrDefault(ind, new TreeSet<>())),
                    new ArrayList<>(yearsByIndicator.get(ind))));
        }

        return new DashboardDto(runId, indicators, series);
    }

    private static boolean isPerArea(String unit) {
        if (unit == null) return false;
        return unit.toLowerCase(Locale.ROOT).replace(" ", "").contains("/ha");
    }

    /** Convertit une surface en hectares selon l'unité (m2 → /10000, sinon supposée ha). */
    private static double toHectares(double value, String unit) {
        if (unit == null) return value;
        String u = unit.toLowerCase(Locale.ROOT).replace(" ", "");
        if (u.equals("m2") || u.equals("m²")) return value / 10_000.0;
        return value;
    }

    private static final class Acc {
        double sum;
        double weighted;
        int count;
        boolean hasSurface;
    }
}
