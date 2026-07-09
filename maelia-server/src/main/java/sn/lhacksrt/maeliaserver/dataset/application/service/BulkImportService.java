package sn.lhacksrt.maeliaserver.dataset.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.port.in.CatalogUseCase;
import sn.lhacksrt.maeliaserver.dataset.application.csv.CsvImportService;
import sn.lhacksrt.maeliaserver.dataset.application.validation.ValidationEngine;
import sn.lhacksrt.maeliaserver.dataset.domain.model.Dataset;
import sn.lhacksrt.maeliaserver.dataset.domain.model.DatasetFile;
import sn.lhacksrt.maeliaserver.dataset.domain.model.ValidationReport;
import sn.lhacksrt.maeliaserver.dataset.domain.port.out.DatasetFileRepository;
import sn.lhacksrt.maeliaserver.dataset.domain.port.out.DatasetRepository;
import sn.lhacksrt.maeliaserver.dataset.domain.port.out.FileStoragePort;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Initialisation en masse : l'utilisateur fournit une archive ZIP contenant un maximum
 * de fichiers d'entrée (CSV et shapefiles). Chaque entrée est appariée au catalogue par
 * son nom de fichier — exact d'abord, puis par motif d'instance ({@code fileNamePattern})
 * pour les types multi-instance (ex. 2018.csv → série climatique AAAA.csv, chaque fichier
 * devenant un dataset d'instance distinct). Les CSV sont importés puis validés, les
 * shapefiles (groupés par basename : .shp + .shx + .dbf + annexes) sont stockés comme un
 * upload SHP classique. Chaque entrée est traitée dans sa propre transaction : un
 * ré-import remplace l'existant, et l'échec d'un fichier (y compris au flush SQL)
 * devient un statut ERREUR dans le rapport sans annuler les autres.
 */
@Service
public class BulkImportService {

    private static final Logger log = LoggerFactory.getLogger(BulkImportService.class);

    private static final Set<String> SHP_EXTENSIONS =
            Set.of("shp", "shx", "dbf", "prj", "cpg", "sbn", "sbx", "qix");
    private static final Set<String> SHP_REQUIRED = Set.of("shp", "shx", "dbf");

    public record EntryReport(
            String entryName,
            String dataSpecId,
            String fileType,
            String status,     // VALIDE | INVALIDE | IGNORE | ERREUR
            String message,
            int recordCount
    ) {}

    public record BulkImportReport(
            int totalEntries,
            int imported,
            int invalid,
            int ignored,
            int errors,
            List<EntryReport> entries
    ) {}

    private final CatalogUseCase catalog;
    private final CsvImportService csvImport;
    private final ValidationEngine validator;
    private final DatasetRepository datasetRepository;
    private final DatasetFileRepository fileRepository;
    private final FileStoragePort storage;
    private final TransactionTemplate tx;

    public BulkImportService(CatalogUseCase catalog,
                             CsvImportService csvImport,
                             ValidationEngine validator,
                             DatasetRepository datasetRepository,
                             DatasetFileRepository fileRepository,
                             FileStoragePort storage,
                             PlatformTransactionManager transactionManager) {
        this.catalog = catalog;
        this.csvImport = csvImport;
        this.validator = validator;
        this.datasetRepository = datasetRepository;
        this.fileRepository = fileRepository;
        this.storage = storage;
        this.tx = new TransactionTemplate(transactionManager);
    }

    // PAS de @Transactional global : chaque entrée commit/rollback indépendamment (tx.execute).
    // Sinon une violation de contrainte au flush (ré-import) empoisonnait la transaction après
    // les try/catch par entrée et faisait échouer toute la requête en 500.
    public BulkImportReport importZip(UUID projectId, MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        if (!original.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new IllegalArgumentException("Fournir une archive .zip contenant les fichiers d'entrée.");
        }

        // 1) Lecture de l'archive : basename → contenu (ordre conservé, doublons = dernier gagne).
        Map<String, byte[]> entries = readEntries(file);

        // 2) Index catalogue : nom de fichier (minuscule) → DataSpecs candidats.
        List<DataSpec> allSpecs = catalog.getAllDataSpecs();
        Map<String, List<DataSpec>> byFileName = new LinkedHashMap<>();
        for (DataSpec spec : allSpecs) {
            byFileName.computeIfAbsent(spec.fileName().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(spec);
        }

        List<EntryReport> reports = new ArrayList<>();

        // 3) Shapefiles : regroupement des annexes par basename, traités via l'entrée .shp.
        Map<String, Map<String, byte[]>> shpGroups = new LinkedHashMap<>(); // basename → ext → bytes
        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            String ext = extension(e.getKey());
            if (SHP_EXTENSIONS.contains(ext)) {
                shpGroups.computeIfAbsent(stripExtension(e.getKey()).toLowerCase(Locale.ROOT),
                        k -> new LinkedHashMap<>()).put(ext, e.getValue());
            }
        }

        for (Map.Entry<String, Map<String, byte[]>> group : shpGroups.entrySet()) {
            reports.add(importShpGroup(projectId, group.getKey(), group.getValue(), byFileName, allSpecs));
        }

        // 4) CSV : appariement par nom exact.
        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            String ext = extension(e.getKey());
            if (SHP_EXTENSIONS.contains(ext)) continue; // déjà traité via le groupe shapefile
            if (!"csv".equals(ext)) {
                reports.add(new EntryReport(e.getKey(), null, null, "IGNORE",
                        "Extension non gérée (seuls CSV et shapefiles sont importés).", 0));
                continue;
            }
            reports.add(importCsvEntry(projectId, e.getKey(), e.getValue(), byFileName, allSpecs));
        }

        int imported = (int) reports.stream().filter(r -> "VALIDE".equals(r.status())).count();
        int invalid = (int) reports.stream().filter(r -> "INVALIDE".equals(r.status())).count();
        int ignored = (int) reports.stream().filter(r -> "IGNORE".equals(r.status())).count();
        int errors = (int) reports.stream().filter(r -> "ERREUR".equals(r.status())).count();

        log.info("Import ZIP projet {} : {} entrées → {} importés, {} invalides, {} ignorés, {} erreurs",
                projectId, reports.size(), imported, invalid, ignored, errors);

        return new BulkImportReport(reports.size(), imported, invalid, ignored, errors, List.copyOf(reports));
    }

    /* ============================== CSV ============================== */

    private EntryReport importCsvEntry(UUID projectId, String entryName, byte[] bytes,
                                       Map<String, List<DataSpec>> byFileName, List<DataSpec> allSpecs) {
        SpecMatch match = matchCsv(entryName, byFileName, allSpecs);
        if (match == null) {
            return new EntryReport(entryName, null, "CSV", "IGNORE",
                    ignoreReason(entryName, byFileName, allSpecs, "CSV"), 0);
        }
        DataSpec spec = match.spec();
        String instanceKey = match.instanceKey();
        try {
            List<Map<String, Object>> records = csvImport.parse(new ByteArrayInputStream(bytes), spec);
            // Transaction propre à l'entrée : le ré-import remplace le dataset existant, et une
            // erreur SQL (même au flush) reste locale à ce fichier.
            return tx.execute(status -> {
                Dataset dataset = (instanceKey == null
                        ? datasetRepository.findByProjectAndDataSpec(projectId, spec.id())
                        : datasetRepository.findByProjectAndDataSpecInstance(projectId, spec.id(), instanceKey))
                        .orElseGet(() -> Dataset.create(projectId, spec.id(), instanceKey));
                dataset.replaceRecords(records);

                ValidationReport report = validator.validate(spec, dataset.getRecords());
                if (report.valid()) dataset.markValid(); else dataset.markInvalid();
                datasetRepository.save(dataset);
                datasetRepository.saveValidationIssues(dataset.getId(), report.issues());

                String instanceNote = instanceKey != null ? " (instance de " + spec.fileName() + ")" : "";
                return new EntryReport(entryName, spec.id(), "CSV",
                        report.valid() ? "VALIDE" : "INVALIDE",
                        report.valid() ? "Importé et validé" + instanceNote + "."
                                : "Importé" + instanceNote + " mais " + report.errorCount() + " erreur(s) de validation.",
                        records.size());
            });
        } catch (Exception ex) {
            return new EntryReport(entryName, spec.id(), "CSV", "ERREUR",
                    "Échec de l'import : " + ex.getMessage(), 0);
        }
    }

    /**
     * Appariement d'une entrée CSV : nom exact d'abord, puis motif d'instance
     * ({@link DataSpec#matchesInstanceFileName}) — le nom du fichier devient alors la clé
     * d'instance du dataset (ex. "2018.csv" pour la série climatique AAAA.csv).
     */
    private SpecMatch matchCsv(String entryName, Map<String, List<DataSpec>> byFileName, List<DataSpec> allSpecs) {
        DataSpec exact = matchUnique(entryName, byFileName, "CSV");
        if (exact != null) return new SpecMatch(exact, null);
        List<DataSpec> byPattern = allSpecs.stream()
                .filter(s -> "CSV".equalsIgnoreCase(s.fileType()))
                .filter(s -> s.matchesInstanceFileName(entryName))
                .toList();
        return byPattern.size() == 1 ? new SpecMatch(byPattern.get(0), entryName) : null;
    }

    private record SpecMatch(DataSpec spec, String instanceKey) {}

    /* ============================ Shapefile ============================ */

    private EntryReport importShpGroup(UUID projectId, String baseName, Map<String, byte[]> byExt,
                                       Map<String, List<DataSpec>> byFileName, List<DataSpec> allSpecs) {
        String shpName = baseName + ".shp";
        DataSpec spec = matchUnique(shpName, byFileName, "SHP");
        if (spec == null) {
            return new EntryReport(shpName, null, "SHP", "IGNORE",
                    ignoreReason(shpName, byFileName, allSpecs, "SHP"), 0);
        }
        if (!byExt.keySet().containsAll(SHP_REQUIRED)) {
            return new EntryReport(shpName, spec.id(), "SHP", "ERREUR",
                    "Shapefile incomplet : requiert .shp, .shx et .dbf (trouvé : " + byExt.keySet() + ").", 0);
        }
        try {
            // Renommage sur le basename attendu par le modèle, stockage MinIO (même logique que l'upload SHP).
            String targetBase = stripExtension(spec.fileName());
            List<DatasetFile> files = byExt.entrySet().stream().map(entry -> {
                String finalName = targetBase + "." + entry.getKey();
                String objectKey = "projects/" + projectId + "/shp/" + spec.id() + "/" + finalName;
                byte[] bytes = entry.getValue();
                storage.put(objectKey, new ByteArrayInputStream(bytes), bytes.length, contentType(entry.getKey()));
                return DatasetFile.create(projectId, spec.id(), finalName, objectKey, bytes.length,
                        contentType(entry.getKey()));
            }).toList();

            // Transaction propre à l'entrée (cf. importCsvEntry).
            return tx.execute(status -> {
                fileRepository.replaceAll(projectId, spec.id(), files);

                Dataset dataset = datasetRepository.findByProjectAndDataSpec(projectId, spec.id())
                        .orElseGet(() -> Dataset.create(projectId, spec.id()));
                dataset.markValid();
                datasetRepository.save(dataset);

                return new EntryReport(shpName, spec.id(), "SHP", "VALIDE",
                        "Shapefile stocké (" + byExt.keySet() + ").", 0);
            });
        } catch (Exception ex) {
            return new EntryReport(shpName, spec.id(), "SHP", "ERREUR",
                    "Échec du stockage : " + ex.getMessage(), 0);
        }
    }

    /* ============================== Helpers ============================== */

    /** DataSpec unique correspondant au nom (même type de fichier), sinon null. */
    private DataSpec matchUnique(String entryName, Map<String, List<DataSpec>> byFileName, String fileType) {
        List<DataSpec> candidates = byFileName.getOrDefault(entryName.toLowerCase(Locale.ROOT), List.of())
                .stream().filter(s -> fileType.equalsIgnoreCase(s.fileType())).toList();
        return candidates.size() == 1 ? candidates.get(0) : null;
    }

    private String ignoreReason(String entryName, Map<String, List<DataSpec>> byFileName,
                                List<DataSpec> allSpecs, String fileType) {
        long exact = byFileName.getOrDefault(entryName.toLowerCase(Locale.ROOT), List.of())
                .stream().filter(s -> fileType.equalsIgnoreCase(s.fileType())).count();
        if (exact > 1) {
            return "Plusieurs fichiers du catalogue portent ce nom : appariement ambigu, importez-le individuellement.";
        }
        long byPattern = allSpecs.stream()
                .filter(s -> fileType.equalsIgnoreCase(s.fileType()))
                .filter(s -> s.matchesInstanceFileName(entryName))
                .count();
        if (byPattern > 1) {
            return "Plusieurs types multi-instance reconnaissent ce nom (motifs ambigus) : importez-le individuellement.";
        }
        return "Aucun fichier du catalogue ne porte ce nom ni ne correspond à un motif d'instance.";
    }

    /** Lit les entrées de l'archive : basename → contenu (répertoires et fichiers système ignorés). */
    private static Map<String, byte[]> readEntries(MultipartFile file) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = baseName(entry.getName());
                if (name.isBlank() || name.startsWith(".") || entry.getName().contains("__MACOSX")) continue;
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                zip.transferTo(out);
                entries.put(name, out.toByteArray());
            }
        }
        return entries;
    }

    /** Dernier segment du chemin d'entrée (neutralise les chemins et le zip-slip). */
    private static String baseName(String entryName) {
        String normalized = entryName.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    private static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String contentType(String ext) {
        return switch (ext) {
            case "prj", "cpg" -> "text/plain";
            default -> "application/octet-stream";
        };
    }
}
