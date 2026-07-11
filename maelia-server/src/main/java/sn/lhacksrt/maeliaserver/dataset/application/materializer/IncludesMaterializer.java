package sn.lhacksrt.maeliaserver.dataset.application.materializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.port.in.CatalogUseCase;
import sn.lhacksrt.maeliaserver.dataset.domain.model.Dataset;
import sn.lhacksrt.maeliaserver.dataset.domain.model.DatasetFile;
import sn.lhacksrt.maeliaserver.dataset.domain.port.out.DatasetFileRepository;
import sn.lhacksrt.maeliaserver.dataset.domain.port.out.DatasetRepository;
import sn.lhacksrt.maeliaserver.dataset.domain.port.out.FileStoragePort;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Régénère l'arborescence includes/ dans le volume partagé gama-workspace
 * à partir des datasets validés d'un projet.
 *
 * Structure cible (sous maelia/ pour rester dans le périmètre du modèle GAMA) :
 *   gama-workspace/maelia/projects/{projectId}/includes/{spec.folder}/{spec.fileName}
 *
 * Chaque matérialisation repart du socle {@code gama.base-includes} (par défaut
 * gama-workspace/maelia/includes : l'ensemble complet des fichiers à fournir),
 * puis écrase avec les CSV saisis/modifiés par l'utilisateur (datasets VALIDE).
 */
@Component
public class IncludesMaterializer {

    private static final Logger log = LoggerFactory.getLogger(IncludesMaterializer.class);

    private final DatasetRepository datasetRepository;
    private final DatasetFileRepository datasetFileRepository;
    private final FileStoragePort fileStorage;
    private final CatalogUseCase catalog;
    private final sn.lhacksrt.maeliaserver.dataset.application.csv.CsvOrientationCodec codec;

    @Value("${gama.workspace:./gama-workspace}")
    private String gamaWorkspace;

    /**
     * Verrous par projet : sérialise l'écriture des includes d'un même projet. Ceinture et
     * bretelles avec le refus d'un 2e run actif (LaunchRunService) — évite toute course sur
     * {@code projects/{id}/includes/} si deux matérialisations du même projet se chevauchaient.
     */
    private final java.util.concurrent.ConcurrentHashMap<UUID, Object> projectLocks =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Arborescence d'includes de référence (SHP + fichiers de base) copiée comme socle
     * avant d'écraser avec les données saisies. Vide = désactivé (seuls les CSV saisis sont écrits).
     */
    @Value("${gama.base-includes:}")
    private String baseIncludes;

    public IncludesMaterializer(DatasetRepository datasetRepository,
                                DatasetFileRepository datasetFileRepository,
                                FileStoragePort fileStorage,
                                CatalogUseCase catalog,
                                sn.lhacksrt.maeliaserver.dataset.application.csv.CsvOrientationCodec codec) {
        this.datasetRepository = datasetRepository;
        this.datasetFileRepository = datasetFileRepository;
        this.fileStorage = fileStorage;
        this.catalog = catalog;
        this.codec = codec;
    }

    /**
     * Matérialise les includes d'un projet : (1) copie le socle de référence si configuré
     * (fournit les SHP et les fichiers non saisis), puis (2) écrase avec les CSV saisis validés.
     * @return chemin racine de l'arborescence générée
     */
    public Path materialize(UUID projectId) throws IOException {
        Object lock = projectLocks.computeIfAbsent(projectId, k -> new Object());
        synchronized (lock) {
            return materializeLocked(projectId);
        }
    }

    private Path materializeLocked(UUID projectId) throws IOException {
        Path projectRoot = Paths.get(gamaWorkspace, "maelia", "projects", projectId.toString(), "includes");
        Files.createDirectories(projectRoot);

        // (1) Socle : includes de référence (SHP, etc.). Best-effort.
        if (baseIncludes != null && !baseIncludes.isBlank()) {
            Path base = Paths.get(baseIncludes);
            if (Files.isDirectory(base)) {
                copyTree(base, projectRoot);
                log.info("Base includes copied from {} for project {}", base, projectId);
            } else {
                log.warn("gama.base-includes={} introuvable, socle ignoré", baseIncludes);
            }
        }

        // (2) Overlay CSV : datasets VALIDE non vides.
        List<Dataset> datasets = datasetRepository.findByProject(projectId);
        int csv = 0;
        for (Dataset dataset : datasets) {
            if (!dataset.getStatus().name().equals("VALIDE") || dataset.getRecords().isEmpty()) continue;
            DataSpec spec = catalog.getDataSpec(dataset.getDataSpecId()).orElse(null);
            if (spec == null) continue;

            if ("CSV".equals(spec.fileType())) {
                writeCsv(projectRoot, spec, dataset);
                csv++;
            }
        }

        // (3) Overlay SHP (C8) : fichiers uploadés par l'utilisateur, relus depuis MinIO,
        // écrits sous includes/{spec.folder}/ par-dessus ceux du socle.
        int shp = writeUploadedFiles(projectRoot, projectId);

        log.info("Materialized project {} : {} CSV écrit(s), {} fichier(s) SHP uploadé(s)", projectId, csv, shp);
        return projectRoot;
    }

    /** Écrit les fichiers uploadés (SHP et compagnons) dans l'arborescence du projet. */
    private int writeUploadedFiles(Path projectRoot, UUID projectId) throws IOException {
        int written = 0;
        for (DatasetFile file : datasetFileRepository.findByProject(projectId)) {
            DataSpec spec = catalog.getDataSpec(file.dataSpecId()).orElse(null);
            if (spec == null) {
                log.warn("Fichier uploadé ignoré (DataSpec {} disparu) : {}", file.dataSpecId(), file.fileName());
                continue;
            }
            Path dir = projectRoot.resolve(spec.folder());
            Files.createDirectories(dir);
            try (InputStream in = fileStorage.get(file.objectKey())) {
                Files.copy(in, dir.resolve(file.fileName()), StandardCopyOption.REPLACE_EXISTING);
                written++;
            }
        }
        return written;
    }

    /** Copie récursive d'une arborescence (écrase les fichiers existants). */
    private void copyTree(Path source, Path target) throws IOException {
        try (java.util.stream.Stream<Path> walk = Files.walk(source)) {
            for (Path src : (Iterable<Path>) walk::iterator) {
                Path dest = target.resolve(source.relativize(src).toString());
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(src, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void writeCsv(Path root, DataSpec spec, Dataset dataset) throws IOException {
        Path dir = root.resolve(spec.folder());
        Files.createDirectories(dir);
        // Types multi-instance : l'instanceKey est le nom réel du fichier (ex. 2018.csv pour
        // la spec AAAA.csv) ; sans instance, le nom de la spec s'applique.
        String fileName = dataset.getInstanceKey() != null ? dataset.getInstanceKey() : spec.fileName();
        Path file = dir.resolve(fileName);
        List<Map<String, Object>> records = dataset.getRecords();

        // Le codec respecte l'orientation (colonnes/lignes), le délimiteur du DataSpec (';' pour
        // MAELIA) et le séparateur de liste des champs.
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            codec.write(writer, spec, records);
        }
    }
}
