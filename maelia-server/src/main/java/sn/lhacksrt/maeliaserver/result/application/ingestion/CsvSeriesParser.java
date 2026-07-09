package sn.lhacksrt.maeliaserver.result.application.ingestion;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import sn.lhacksrt.maeliaserver.result.domain.model.ResultValue;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Parseur best-effort de CSV de sortie vers des valeurs d'indicateurs.
 * Classe pure (sans Spring) pour rester testable unitairement (M6).
 *
 * Heuristique : une colonne « date » fournit l'axe temporel ; une colonne de zone
 * (ID_ZH/BVe…) fournit la spatialisation ; toute autre colonne numérique devient un indicateur.
 * Sans colonne date, l'index de ligne sert d'axe (cycle).
 *
 * Perf : la variante {@link #parse(UUID, Reader)} lit en flux (Commons CSV parse les
 * enregistrements paresseusement) — on ne charge jamais tout le fichier en mémoire.
 * L'ingestion est plafonnée à {@link #MAX_VALUES} valeurs par fichier.
 */
public final class CsvSeriesParser {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    private static final Set<String> ZONE_KEYS =
            Set.of("zone", "id_zh", "bve", "no. zh maelia", "no.zh maelia", "code_zone");

    private static final int MAX_VALUES = 50_000;
    private static final char BOM = '﻿';

    private CsvSeriesParser() {}

    /** Variante chaîne (pratique pour les tests). */
    public static List<ResultValue> parse(UUID runId, String content) {
        return parse(runId, new StringReader(content == null ? "" : content));
    }

    /** Variante flux (utilisée en production sur les fichiers, sans tout charger en mémoire). */
    public static List<ResultValue> parse(UUID runId, Reader reader) {
        List<ResultValue> out = new ArrayList<>();
        try (BufferedReader br = (reader instanceof BufferedReader b) ? b : new BufferedReader(reader)) {
            // saute un éventuel BOM UTF-8
            br.mark(4);
            int first = br.read();
            if (first != BOM && first != -1) br.reset();

            // Saute le préambule des sorties MAELIA (lignes `detailSimulation` sans délimiteur)
            // jusqu'à la vraie ligne d'entête (1re ligne contenant un délimiteur).
            String headerLine = null;
            while (true) {
                br.mark(1 << 16);
                String line = br.readLine();
                if (line == null) return out;          // pas d'entête trouvée
                if (containsDelimiter(line)) {          // entête réelle
                    headerLine = line;
                    br.reset();                         // remet l'entête pour le CSVParser
                    break;
                }
                // sinon : ligne de préambule, consommée (on n'appelle pas reset)
            }
            char delimiter = detectDelimiterFromLine(headerLine);

            CSVFormat fmt = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setIgnoreSurroundingSpaces(true)
                    .setAllowMissingColumnNames(true)
                    .build();

            try (CSVParser parser = CSVParser.parse(br, fmt)) {
                List<String> headers = parser.getHeaderNames();
                String dateCol = headers.stream()
                        .filter(h -> h != null && h.toLowerCase().contains("date")).findFirst().orElse(null);
                String zoneCol = headers.stream()
                        .filter(h -> h != null && ZONE_KEYS.contains(h.toLowerCase().trim())).findFirst().orElse(null);

                int rowIdx = 0;
                for (CSVRecord rec : parser) {
                    LocalDate date = dateCol != null ? tryDate(safeGet(rec, dateCol)) : null;
                    String zone = zoneCol != null ? blankToNull(safeGet(rec, zoneCol)) : null;
                    for (String h : headers) {
                        if (h == null || h.equals(dateCol) || h.equals(zoneCol)) continue;
                        Double v = tryDouble(safeGet(rec, h));
                        if (v == null) continue;
                        Integer cycle = date == null ? rowIdx : null;
                        out.add(ResultValue.of(runId, h, zone, date, cycle, v, null));
                        if (out.size() >= MAX_VALUES) return out;
                    }
                    rowIdx++;
                }
            }
        } catch (Exception ignored) {
            // best-effort : on retourne ce qui a pu être extrait
        }
        return out;
    }

    private static boolean containsDelimiter(String line) {
        return line.indexOf(';') >= 0 || line.indexOf(',') >= 0 || line.indexOf('\t') >= 0;
    }

    static char detectDelimiterFromLine(String firstLine) {
        if (firstLine == null) return ';';
        long semi = firstLine.chars().filter(c -> c == ';').count();
        long comma = firstLine.chars().filter(c -> c == ',').count();
        long tab = firstLine.chars().filter(c -> c == '\t').count();
        if (tab > semi && tab > comma) return '\t';
        return semi >= comma ? ';' : ',';
    }

    private static String safeGet(CSVRecord rec, String col) {
        try { return rec.isMapped(col) ? rec.get(col) : null; }
        catch (Exception e) { return null; }
    }

    static LocalDate tryDate(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim();
        for (DateTimeFormatter f : DATE_FORMATS) {
            try { return LocalDate.parse(v, f); } catch (Exception ignored) {}
        }
        return null;
    }

    static Double tryDouble(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isEmpty() || v.equalsIgnoreCase("NA") || v.equalsIgnoreCase("NaN")
                || v.equalsIgnoreCase("null") || v.equals("[NA]")) return null;
        try { return Double.parseDouble(v); } catch (NumberFormatException ignored) {}
        // décimale à la virgule (si le séparateur de colonnes était le ;)
        try { return Double.parseDouble(v.replace(',', '.')); } catch (NumberFormatException ignored) {}
        return null;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
