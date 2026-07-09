package sn.lhacksrt.maeliaserver.dataset.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.port.in.CatalogUseCase;
import sn.lhacksrt.maeliaserver.dataset.domain.model.Dataset;
import sn.lhacksrt.maeliaserver.dataset.domain.model.DatasetFile;
import sn.lhacksrt.maeliaserver.dataset.domain.port.out.DatasetFileRepository;
import sn.lhacksrt.maeliaserver.dataset.domain.port.out.DatasetRepository;
import sn.lhacksrt.maeliaserver.dataset.domain.port.out.FileStoragePort;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * C8 — Upload des shapefiles par l'utilisateur.
 *
 * L'utilisateur fournit une archive ZIP contenant le jeu de fichiers du shapefile
 * (.shp + .shx + .dbf, et optionnellement .prj/.cpg/...). Chaque fichier est renommé
 * sur le basename attendu par le modèle (DataSpec.fileName) puis stocké dans MinIO.
 * À la matérialisation, ces fichiers écrasent ceux du socle (IncludesMaterializer).
 * Le dataset correspondant passe VALIDE (pas de validation en grille pour les SHP).
 */
@Service
public class ShpUploadService {

    private static final Logger log = LoggerFactory.getLogger(ShpUploadService.class);

    /** Extensions du jeu shapefile acceptées dans l'archive. */
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("shp", "shx", "dbf", "prj", "cpg", "sbn", "sbx", "qix");

    private static final Set<String> REQUIRED_EXTENSIONS = Set.of("shp", "shx", "dbf");

    private final CatalogUseCase catalog;
    private final DatasetRepository datasetRepository;
    private final DatasetFileRepository fileRepository;
    private final FileStoragePort storage;

    public ShpUploadService(CatalogUseCase catalog,
                            DatasetRepository datasetRepository,
                            DatasetFileRepository fileRepository,
                            FileStoragePort storage) {
        this.catalog = catalog;
        this.datasetRepository = datasetRepository;
        this.fileRepository = fileRepository;
        this.storage = storage;
    }

    @Transactional
    public List<DatasetFile> upload(UUID projectId, String dataSpecId, MultipartFile file) throws IOException {
        DataSpec spec = catalog.getDataSpec(dataSpecId)
                .orElseThrow(() -> new IllegalArgumentException("DataSpec inconnu : " + dataSpecId));
        if (!"SHP".equalsIgnoreCase(spec.fileType())) {
            throw new IllegalArgumentException(
                    "Le DataSpec " + dataSpecId + " n'est pas de type SHP (" + spec.fileType() + ")");
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        if (!original.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new IllegalArgumentException(
                    "Fournir une archive .zip contenant le jeu de fichiers du shapefile (.shp, .shx, .dbf, …)");
        }

        // extension (minuscule) → contenu, dans l'ordre de l'archive
        Map<String, byte[]> byExtension = readZipEntries(file);

        if (!byExtension.keySet().containsAll(REQUIRED_EXTENSIONS)) {
            throw new IllegalArgumentException(
                    "Archive incomplète : le shapefile requiert au minimum .shp, .shx et .dbf (trouvé : "
                            + byExtension.keySet() + ")");
        }

        // Renommage sur le basename attendu par le modèle (ex. ilots.shp → ilots.dbf, ilots.prj…)
        String baseName = stripExtension(spec.fileName());
        List<DatasetFile> files = byExtension.entrySet().stream().map(entry -> {
            String finalName = baseName + "." + entry.getKey();
            String objectKey = "projects/" + projectId + "/shp/" + dataSpecId + "/" + finalName;
            byte[] bytes = entry.getValue();
            storage.put(objectKey, new ByteArrayInputStream(bytes), bytes.length, contentType(entry.getKey()));
            return DatasetFile.create(projectId, dataSpecId, finalName, objectKey, bytes.length, contentType(entry.getKey()));
        }).toList();

        fileRepository.replaceAll(projectId, dataSpecId, files);

        // Le SHP fourni couvre le DataSpec : le dataset passe VALIDE (complétude projet).
        Dataset dataset = datasetRepository.findByProjectAndDataSpec(projectId, dataSpecId)
                .orElseGet(() -> Dataset.create(projectId, dataSpecId));
        dataset.markValid();
        datasetRepository.save(dataset);

        log.info("SHP uploadé pour project={} spec={} : {} fichier(s) ({})",
                projectId, dataSpecId, files.size(), byExtension.keySet());
        return files;
    }

    @Transactional(readOnly = true)
    public List<DatasetFile> listFiles(UUID projectId, String dataSpecId) {
        return fileRepository.findByProjectAndDataSpec(projectId, dataSpecId);
    }

    /** Lit les entrées utiles de l'archive : une par extension autorisée, doublon = erreur. */
    private static Map<String, byte[]> readZipEntries(MultipartFile file) throws IOException {
        Map<String, byte[]> byExtension = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String name = baseName(entry.getName());
                if (name.startsWith(".") || name.startsWith("__MACOSX")) continue;
                String ext = extension(name);
                if (!ALLOWED_EXTENSIONS.contains(ext)) continue;
                if (byExtension.containsKey(ext)) {
                    throw new IllegalArgumentException(
                            "Archive ambiguë : plusieurs fichiers ." + ext + " (un seul shapefile par archive)");
                }
                byExtension.put(ext, readAll(zip));
            }
        }
        return byExtension;
    }

    private static byte[] readAll(ZipInputStream zip) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        zip.transferTo(out);
        return out.toByteArray();
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
