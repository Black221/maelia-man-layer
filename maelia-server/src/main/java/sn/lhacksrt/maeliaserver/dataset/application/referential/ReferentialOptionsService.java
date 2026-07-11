package sn.lhacksrt.maeliaserver.dataset.application.referential;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.port.in.CatalogUseCase;
import sn.lhacksrt.maeliaserver.dataset.domain.model.Dataset;
import sn.lhacksrt.maeliaserver.dataset.domain.model.DatasetFile;
import sn.lhacksrt.maeliaserver.dataset.domain.port.out.DatasetFileRepository;
import sn.lhacksrt.maeliaserver.dataset.domain.port.out.DatasetRepository;
import sn.lhacksrt.maeliaserver.dataset.domain.port.out.FileStoragePort;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Résout les valeurs proposées pour un paramètre de scénario « select depuis données »
 * (sélecteurs d'ID / de valeurs). Source configurable par paramètre (options_source) :
 * <ul>
 *   <li>{@code COLUMN} (défaut) : valeurs distinctes d'une colonne (options_column, sinon
 *       1er champ) d'un DataSpec. Lit les données SAISIES du projet si présentes, sinon le
 *       <b>socle</b> ({@code gama.base-includes}) — marche donc même sans upload. CSV et SHP
 *       (le .dbf est lu depuis MinIO si uploadé, sinon depuis le socle).</li>
 *   <li>{@code COLUMN_HEADERS} : noms de colonnes (hors 1re) — ex. cultures d'especesCultivees.</li>
 *   <li>{@code INSTANCE_KEYS} : clés d'instance d'un DataSpec multi-instance (ex. prixVentesXX).</li>
 * </ul>
 * Best-effort : toute erreur de lecture renvoie une liste vide (le champ reste saisissable).
 */
@Service
public class ReferentialOptionsService {

    private static final Logger log = LoggerFactory.getLogger(ReferentialOptionsService.class);

    private final CatalogUseCase catalog;
    private final DatasetRepository datasetRepository;
    private final DatasetFileRepository fileRepository;
    private final FileStoragePort storage;

    @Value("${gama.base-includes:}")
    private String baseIncludes;

    public ReferentialOptionsService(CatalogUseCase catalog,
                                     DatasetRepository datasetRepository,
                                     DatasetFileRepository fileRepository,
                                     FileStoragePort storage) {
        this.catalog = catalog;
        this.datasetRepository = datasetRepository;
        this.fileRepository = fileRepository;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<String> options(UUID projectId, String dataSpecId, String column, String source) {
        DataSpec spec = catalog.getDataSpec(dataSpecId).orElse(null);
        if (spec == null) return List.of();
        String src = (source == null || source.isBlank()) ? "COLUMN" : source.trim().toUpperCase();
        try {
            return switch (src) {
                case "INSTANCE_KEYS" -> instanceKeys(projectId, spec);
                case "COLUMN_HEADERS" -> columnHeaders(spec);
                default -> columnValues(projectId, spec, column);
            };
        } catch (Exception e) {
            log.warn("Référentiel {} (source={}, col={}) illisible : {}", dataSpecId, src, column, e.getMessage());
            return List.of();
        }
    }

    // --- COLUMN : valeurs distinctes d'une colonne ---------------------------------------------

    private List<String> columnValues(UUID projectId, DataSpec spec, String column) throws Exception {
        String col = (column != null && !column.isBlank())
                ? column.trim()
                : firstFieldLabel(spec);
        if (col == null) return List.of();

        if ("SHP".equalsIgnoreCase(spec.fileType())) {
            byte[] dbf = dbfBytes(projectId, spec);
            return dbf == null ? List.of() : DbfReader.distinctColumn(dbf, col);
        }

        // CSV : données saisies du projet si présentes, sinon socle.
        Dataset ds = datasetRepository.findByProjectAndDataSpec(projectId, spec.id()).orElse(null);
        if (ds != null && ds.getRecords() != null && !ds.getRecords().isEmpty()) {
            return distinct(ds.getRecords().stream().map(r -> str(r.get(col))));
        }
        Path socle = soclePath(spec, spec.fileName());
        return (socle != null && Files.isReadable(socle))
                ? readCsvColumn(socle, spec.effectiveDelimiter(), col)
                : List.of();
    }

    // --- COLUMN_HEADERS : noms de colonnes (hors 1re) ------------------------------------------

    private List<String> columnHeaders(DataSpec spec) throws Exception {
        Path socle = soclePath(spec, spec.fileName());
        if (socle == null || !Files.isReadable(socle)) return List.of();
        try (Reader reader = Files.newBufferedReader(socle, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setDelimiter(spec.effectiveDelimiter()).build().parse(reader)) {
            for (CSVRecord rec : parser) {
                List<String> headers = new ArrayList<>();
                rec.forEach(headers::add);
                // 1re colonne = clé de ligne (ID_ESPECE…), on ne propose que les suivantes (cultures).
                return headers.stream().skip(1).map(String::trim)
                        .filter(s -> !s.isEmpty()).distinct().toList();
            }
        }
        return List.of();
    }

    // --- INSTANCE_KEYS : instances d'un DataSpec multi-instance --------------------------------

    private List<String> instanceKeys(UUID projectId, DataSpec spec) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        // Instances saisies dans le projet.
        for (Dataset ds : datasetRepository.findByProject(projectId)) {
            if (spec.id().equals(ds.getDataSpecId()) && ds.getInstanceKey() != null) {
                keys.add(deriveInstanceId(spec, ds.getInstanceKey()));
            }
        }
        // Complète avec les instances du socle (fichiers correspondant au motif).
        Path dir = socleDir(spec);
        if (dir != null && Files.isDirectory(dir)) {
            try (var files = Files.list(dir)) {
                files.filter(Files::isRegularFile)
                        .map(p -> p.getFileName().toString())
                        .filter(spec::matchesInstanceFileName)
                        .forEach(name -> keys.add(deriveInstanceId(spec, name)));
            } catch (Exception e) {
                log.debug("Scan socle instances {} ignoré : {}", spec.id(), e.getMessage());
            }
        }
        keys.removeIf(k -> k == null || k.isBlank());
        return new ArrayList<>(keys);
    }

    /** Extrait l'ID d'instance depuis un nom de fichier via le gabarit fileName (ex.
     *  "prixVentes(ID).csv" + "prixVentesSC1.csv" → "SC1") ; sinon retourne le nom sans extension. */
    private static String deriveInstanceId(DataSpec spec, String fileName) {
        String template = spec.fileName();
        if (template != null && template.contains("(ID)")) {
            int cut = template.indexOf("(ID)");
            String prefix = template.substring(0, cut);
            String suffix = template.substring(cut + 4);
            String n = fileName;
            if (prefix.length() + suffix.length() <= n.length()
                    && n.regionMatches(true, 0, prefix, 0, prefix.length())
                    && n.regionMatches(true, n.length() - suffix.length(), suffix, 0, suffix.length())) {
                return n.substring(prefix.length(), n.length() - suffix.length());
            }
        }
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    // --- Lecture bas niveau --------------------------------------------------------------------

    /** Octets du .dbf : fichier uploadé (MinIO) prioritaire, sinon socle. */
    private byte[] dbfBytes(UUID projectId, DataSpec spec) throws Exception {
        String dbfName = stripExtension(spec.fileName()) + ".dbf";
        for (DatasetFile f : fileRepository.findByProjectAndDataSpec(projectId, spec.id())) {
            if (f.fileName().equalsIgnoreCase(dbfName)) {
                try (InputStream in = storage.get(f.objectKey())) {
                    return in.readAllBytes();
                }
            }
        }
        Path socle = soclePath(spec, dbfName);
        return (socle != null && Files.isReadable(socle)) ? Files.readAllBytes(socle) : null;
    }

    private List<String> readCsvColumn(Path file, char delimiter, String column) throws Exception {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setDelimiter(delimiter).setHeader().setSkipHeaderRecord(true)
                     .setIgnoreSurroundingSpaces(true).setAllowMissingColumnNames(true).build()
                     .parse(reader)) {
            if (!parser.getHeaderMap().containsKey(column)) return List.of();
            LinkedHashSet<String> values = new LinkedHashSet<>();
            for (CSVRecord rec : parser) {
                String v = rec.isSet(column) ? rec.get(column).trim() : "";
                if (!v.isEmpty()) values.add(v);
            }
            return new ArrayList<>(values);
        }
    }

    private String firstFieldLabel(DataSpec spec) {
        return (spec.fields() != null && !spec.fields().isEmpty())
                ? spec.fields().get(0).label() : null;
    }

    private Path socleDir(DataSpec spec) {
        if (baseIncludes == null || baseIncludes.isBlank()) return null;
        return Paths.get(baseIncludes, spec.folder());
    }

    private Path soclePath(DataSpec spec, String fileName) {
        Path dir = socleDir(spec);
        return dir == null ? null : dir.resolve(fileName);
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static List<String> distinct(java.util.stream.Stream<String> stream) {
        return stream.filter(s -> s != null && !s.isBlank()).map(String::trim)
                .distinct().toList();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
