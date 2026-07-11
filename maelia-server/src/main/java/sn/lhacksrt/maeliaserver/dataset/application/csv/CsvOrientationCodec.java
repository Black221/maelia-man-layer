package sn.lhacksrt.maeliaserver.dataset.application.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.model.FieldSpec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Encode/décode des CSV MAELIA en respectant l'<b>orientation</b> du {@link DataSpec}.
 *
 * <p>Les enregistrements sont toujours manipulés sous forme normalisée
 * ({@code List<Map<label, valeur>>}), indépendamment de l'orientation du fichier :
 * <ul>
 *   <li>{@code FIELDS_AS_COLUMNS} : entête nommée (COLUMN_HEADER) ou positionnel (LINE_NUMBER) ;
 *       chaque ligne = un enregistrement.</li>
 *   <li>{@code FIELDS_AS_ROWS} : transposé ; chaque champ = une ligne (col 0 = clé du champ),
 *       chaque enregistrement = une colonne à partir de {@code matrixValueStartIndex}.</li>
 * </ul>
 */
@Component
public class CsvOrientationCodec {

    /* ============================== LECTURE ============================== */

    public List<Map<String, Object>> read(InputStream csv, DataSpec spec) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8));
        char delimiter = resolveDelimiter(reader, spec);

        if (spec.isTransposed()) {
            return readTransposed(reader, spec, delimiter);
        }
        return readColumnar(reader, spec, delimiter);
    }

    /** FIELDS_AS_COLUMNS : entête (COLUMN_HEADER) ou positionnel (LINE_NUMBER). */
    private List<Map<String, Object>> readColumnar(Reader reader, DataSpec spec, char delimiter) throws IOException {
        boolean header = !"LINE_NUMBER".equals(spec.csvFormat());
        List<FieldSpec> fields = spec.fields();

        CSVFormat.Builder builder = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setIgnoreEmptyLines(true)
                .setTrim(true);
        // Tolérance aux en-têtes MAELIA « imparfaits » : première colonne sans nom (clé de ligne
        // implicite, ex. materiel.csv « ;SIJ;travail ») ou en-têtes dupliqués — sinon Commons CSV
        // lève « A header name is missing » et fait échouer tout l'import.
        if (header) builder.setHeader().setSkipHeaderRecord(true)
                .setAllowMissingColumnNames(true).setAllowDuplicateHeaderNames(true);

        List<Map<String, Object>> records = new ArrayList<>();
        try (CSVParser parser = new CSVParser(reader, builder.build())) {
            for (CSVRecord row : parser) {
                Map<String, Object> record = new LinkedHashMap<>();
                if (header) {
                    for (Map.Entry<String, Integer> h : parser.getHeaderMap().entrySet()) {
                        if (h.getValue() >= row.size()) continue;
                        record.put(h.getKey(), blankToNull(row.get(h.getValue())));
                    }
                } else {
                    for (FieldSpec field : fields) {
                        int pos = field.position() != null ? field.position() : 0;
                        record.put(field.label(), pos < row.size() ? blankToNull(row.get(pos)) : null);
                    }
                }
                if (!record.values().stream().allMatch(v -> v == null)) records.add(record);
            }
        }
        return records;
    }

    /** FIELDS_AS_ROWS : chaque ligne = un champ (col 0 = label) ; chaque colonne de données = un enregistrement. */
    private List<Map<String, Object>> readTransposed(Reader reader, DataSpec spec, char delimiter) throws IOException {
        int start = spec.effectiveMatrixValueStartIndex();

        List<String[]> rows = new ArrayList<>();
        int maxCols = 0;
        CSVFormat fmt = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter).setIgnoreEmptyLines(true).setTrim(true).build();
        try (CSVParser parser = new CSVParser(reader, fmt)) {
            for (CSVRecord row : parser) {
                String[] arr = new String[row.size()];
                for (int i = 0; i < row.size(); i++) arr[i] = row.get(i);
                rows.add(arr);
                maxCols = Math.max(maxCols, arr.length);
            }
        }

        List<Map<String, Object>> records = new ArrayList<>();
        for (int c = start; c < maxCols; c++) {
            Map<String, Object> record = new LinkedHashMap<>();
            for (String[] row : rows) {
                if (row.length == 0) continue;
                String label = row[0];
                if (label == null || label.isBlank()) continue;
                record.put(label, c < row.length ? blankToNull(row[c]) : null);
            }
            if (!record.values().stream().allMatch(v -> v == null)) records.add(record);
        }
        return records;
    }

    /* ============================== ÉCRITURE ============================== */

    public void write(Writer writer, DataSpec spec, List<Map<String, Object>> records) throws IOException {
        char delimiter = spec.effectiveDelimiter();
        List<String> labels = spec.fields().stream().map(FieldSpec::label).toList();

        if (spec.isTransposed()) {
            writeTransposed(writer, spec, records, labels, delimiter);
        } else {
            writeColumnar(writer, spec, records, labels, delimiter);
        }
    }

    private void writeColumnar(Writer writer, DataSpec spec, List<Map<String, Object>> records,
                               List<String> labels, char delimiter) throws IOException {
        boolean header = !"LINE_NUMBER".equals(spec.csvFormat());
        CSVFormat.Builder builder = CSVFormat.DEFAULT.builder().setDelimiter(delimiter);
        if (header) builder.setHeader(labels.toArray(String[]::new));

        try (CSVPrinter printer = new CSVPrinter(writer, builder.build())) {
            for (Map<String, Object> row : records) {
                List<Object> values = labels.stream().map(l -> nullToBlank(row.get(l))).toList();
                printer.printRecord(values);
            }
        }
    }

    /** Transposé : 1 ligne par champ = [label, (colonnes méta vides), valeur_rec1, valeur_rec2, …]. */
    private void writeTransposed(Writer writer, DataSpec spec, List<Map<String, Object>> records,
                                 List<String> labels, char delimiter) throws IOException {
        int start = spec.effectiveMatrixValueStartIndex();

        // Union : champs déclarés au catalogue PUIS toute ligne présente dans les données mais
        // NON déclarée (ordre du fichier préservé). Sans cela, une matrice dont le catalogue est
        // incomplet perd des lignes à la réécriture — ex. Engrais 'plan_epandage' absent des
        // matrix.parameters : la matrice réécrite avait 34 lignes au lieu de 35, GAMA indexait
        // hors bornes (IndexOutOfBoundsException) et le serveur crashait à l'initialisation.
        java.util.LinkedHashSet<String> allLabels = new java.util.LinkedHashSet<>(labels);
        for (Map<String, Object> record : records) allLabels.addAll(record.keySet());

        CSVFormat fmt = CSVFormat.DEFAULT.builder().setDelimiter(delimiter).build();
        try (CSVPrinter printer = new CSVPrinter(writer, fmt)) {
            for (String label : allLabels) {
                List<Object> line = new ArrayList<>();
                line.add(label);
                // colonnes méta entre la clé et les valeurs (non capturées dans le modèle normalisé)
                for (int i = 1; i < start; i++) line.add("");
                for (Map<String, Object> record : records) {
                    line.add(nullToBlank(record.get(label)));
                }
                printer.printRecord(line);
            }
        }
    }

    /* ============================== UTILITAIRES ============================== */

    private static Object blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    private static Object nullToBlank(Object v) {
        return v == null ? "" : v;
    }

    /** Préfère le délimiteur du DataSpec ; bascule sur auto-détection si absent de la 1re ligne. */
    private char resolveDelimiter(BufferedReader reader, DataSpec spec) throws IOException {
        // Saut d'un éventuel BOM UTF-8.
        reader.mark(4);
        int first = reader.read();
        if (first != '﻿' && first != -1) reader.reset();

        char preferred = spec.effectiveDelimiter();
        reader.mark(1 << 16);
        String line = reader.readLine();
        reader.reset();
        if (line == null) return preferred;

        if (line.indexOf(preferred) >= 0) return preferred;
        long semi = line.chars().filter(c -> c == ';').count();
        long comma = line.chars().filter(c -> c == ',').count();
        long tab = line.chars().filter(c -> c == '\t').count();
        if (tab > semi && tab > comma) return '\t';
        if (comma > semi) return ',';
        return semi > 0 ? ';' : preferred;
    }
}
