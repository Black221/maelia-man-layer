package sn.lhacksrt.maeliaserver.result.application.ingestion;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import sn.lhacksrt.maeliaserver.result.domain.model.ResultValue;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ingestion MAELIA-aware des sorties agronomiques par parcelle. Reconnaît les dimensions
 * (annee → year, culture/couvert → category, parcelle → zone) et n'émet que les indicateurs
 * pertinents, avec l'unité extraite de l'en-tête ({@code RECOLTE_rendement[t/ha]} → unité t/ha).
 *
 * Grâce à {@code category} + {@code year}, l'agrégation « rendement par culture au cours du
 * temps » devient possible (impossible avec le parseur générique qui perdait la culture).
 */
@Component
public class MaeliaDimensionalIngestor implements OutputFileIngestor {

    /** Config d'un fichier : colonnes de dimension + indicateurs à émettre (labels sans unité). */
    private record FileSpec(String yearCol, String categoryCol, String zoneCol, Set<String> indicators) {}

    // Clé = nom de fichier en minuscules. Les labels sont comparés sans casse et sans l'unité [..].
    private static final Map<String, FileSpec> SPECS = Map.of(
            "suiviotparparcelle.csv", new FileSpec("annee", "culture", "parcelle",
                    Set.of("recolte_rendement", "irrigation_dose", "irrigation_reelle",
                            "ferti_apportnminreel", "biomasse_export", "n_export")),
            "sorties_eau.csv", new FileSpec("annee", "couvert", "parcelle",
                    Set.of("irrigation", "satisfactionhydrique", "evaporation", "transpiration",
                            "pluie", "percolation")),
            "sorties_ges.csv", new FileSpec("annee", "couvert", "parcelle",
                    Set.of("bilan_net_ges", "delta_corg", "emissions_ferti")),
            "sorties_azote.csv", new FileSpec("annee", "couvert", "parcelle",
                    Set.of("n_lixivie", "satisfactionazote_culture", "n_volatilise_nh3")),
            "surfaceparcelles.csv", new FileSpec(null, null, "idparcelle",
                    Set.of("surface"))
    );

    private static final int MAX_VALUES = 200_000;
    private static final char BOM = '﻿';

    @Override
    public boolean supports(String fileName) {
        return fileName != null && SPECS.containsKey(fileName.toLowerCase(Locale.ROOT));
    }

    @Override
    public List<ResultValue> parse(UUID runId, Reader reader, String fileName) {
        List<ResultValue> out = new ArrayList<>();
        FileSpec spec = fileName != null ? SPECS.get(fileName.toLowerCase(Locale.ROOT)) : null;
        try (BufferedReader br = (reader instanceof BufferedReader b) ? b : new BufferedReader(reader)) {
            br.mark(4);
            int first = br.read();
            if (first != BOM && first != -1) br.reset();

            // Saute le préambule `detailSimulation` (lignes sans délimiteur) jusqu'à l'en-tête.
            String headerLine;
            while (true) {
                br.mark(1 << 16);
                String line = br.readLine();
                if (line == null) return out;
                if (line.indexOf(';') >= 0 || line.indexOf(',') >= 0) { headerLine = line; br.reset(); break; }
            }
            char delimiter = headerLine.chars().filter(c -> c == ';').count()
                    >= headerLine.chars().filter(c -> c == ',').count() ? ';' : ',';

            // Si aucun nom de fichier fourni, tente de deviner la config d'après les colonnes.
            if (spec == null) spec = guessSpec(headerLine, delimiter);
            if (spec == null) return out; // fichier non pris en charge par cet ingestor

            CSVFormat fmt = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter).setHeader().setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true).setIgnoreSurroundingSpaces(true)
                    .setAllowMissingColumnNames(true).setAllowDuplicateHeaderNames(true).build();

            try (CSVParser parser = CSVParser.parse(br, fmt)) {
                List<String> raws = parser.getHeaderNames();
                // Mappe chaque en-tête brut -> (label sans unité, unité).
                Map<String, String[]> parsed = new LinkedHashMap<>();
                for (String raw : raws) parsed.put(raw, splitLabelUnit(raw));

                String yearRaw = findRaw(parsed, spec.yearCol());
                String catRaw = findRaw(parsed, spec.categoryCol());
                String zoneRaw = findRaw(parsed, spec.zoneCol());

                for (CSVRecord rec : parser) {
                    Integer year = yearRaw != null ? tryInt(get(rec, yearRaw)) : null;
                    String category = catRaw != null ? blankToNull(get(rec, catRaw)) : null;
                    String zone = zoneRaw != null ? blankToNull(get(rec, zoneRaw)) : null;

                    for (String raw : raws) {
                        String[] lu = parsed.get(raw);
                        String label = lu[0];
                        String key = label.toLowerCase(Locale.ROOT);
                        if (!spec.indicators().contains(key)) continue;
                        Double v = tryDouble(get(rec, raw));
                        if (v == null) continue;
                        out.add(ResultValue.of(runId, label, category, zone, null, null, year, v, lu[1]));
                        if (out.size() >= MAX_VALUES) return out;
                    }
                }
            }
        } catch (Exception ignored) {
            // best-effort : on retourne ce qui a pu être extrait
        }
        return out;
    }

    /** Devine la config d'après les labels présents (utile quand le nom de fichier est inconnu). */
    private static FileSpec guessSpec(String headerLine, char delimiter) {
        Set<String> labels = new java.util.HashSet<>();
        for (String raw : headerLine.split(String.valueOf(delimiter))) {
            labels.add(splitLabelUnit(raw)[0].toLowerCase(Locale.ROOT));
        }
        for (FileSpec s : SPECS.values()) {
            if (s.indicators().stream().anyMatch(labels::contains)) return s;
        }
        return null;
    }

    /** "RECOLTE_rendement[t/ha]" ou "surface [m2]" -> ["RECOLTE_rendement", "t/ha"]. */
    public static String[] splitLabelUnit(String raw) {
        if (raw == null) return new String[]{"", null};
        String s = raw.trim();
        int lb = s.indexOf('[');
        if (lb >= 0) {
            int rb = s.indexOf(']', lb);
            String label = s.substring(0, lb).trim();
            String unit = rb > lb ? s.substring(lb + 1, rb).trim() : null;
            return new String[]{label, (unit == null || unit.isEmpty()) ? null : unit};
        }
        return new String[]{s, null};
    }

    private static String findRaw(Map<String, String[]> parsed, String label) {
        if (label == null) return null;
        for (Map.Entry<String, String[]> e : parsed.entrySet()) {
            if (e.getValue()[0].equalsIgnoreCase(label)) return e.getKey();
        }
        return null;
    }

    private static String get(CSVRecord rec, String col) {
        try { return rec.isMapped(col) ? rec.get(col) : null; }
        catch (Exception e) { return null; }
    }

    static Integer tryInt(String s) {
        if (s == null || s.isBlank()) return null;
        try { return (int) Double.parseDouble(s.trim().replace(',', '.')); }
        catch (NumberFormatException e) { return null; }
    }

    static Double tryDouble(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isEmpty() || v.equalsIgnoreCase("NA") || v.equalsIgnoreCase("NaN")
                || v.equalsIgnoreCase("null") || v.equals("[NA]")) return null;
        try { return Double.parseDouble(v); } catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(v.replace(',', '.')); } catch (NumberFormatException ignored) {}
        return null;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
