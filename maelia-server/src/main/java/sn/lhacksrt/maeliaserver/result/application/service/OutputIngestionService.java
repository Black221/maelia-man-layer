package sn.lhacksrt.maeliaserver.result.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sn.lhacksrt.maeliaserver.result.application.ingestion.CsvSeriesParser;
import sn.lhacksrt.maeliaserver.result.domain.model.ArtifactType;
import sn.lhacksrt.maeliaserver.result.domain.model.OutputArtifact;
import sn.lhacksrt.maeliaserver.result.domain.model.ResultValue;
import sn.lhacksrt.maeliaserver.result.domain.port.in.IngestOutputsUseCase;
import sn.lhacksrt.maeliaserver.result.domain.port.out.ResultRepository;
import sn.lhacksrt.maeliaserver.result.infrastructure.storage.ArtifactStorage;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Ingestion des sorties d'un run : scanne le répertoire de sortie sur gama-workspace,
 * enregistre chaque fichier comme artefact, et extrait les valeurs d'indicateurs des CSV.
 * Best-effort et résilient : une sortie absente ou un fichier illisible n'interrompt pas le run.
 */
@Service
public class OutputIngestionService implements IngestOutputsUseCase {

    private static final Logger log = LoggerFactory.getLogger(OutputIngestionService.class);

    private final ResultRepository repository;
    private final ArtifactStorage storage;

    public OutputIngestionService(ResultRepository repository, ArtifactStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Override
    public int ingest(UUID runId, UUID projectId) {
        // Emplacement réel des sorties MAELIA (main/log/*/{runId}) ; fallback sur l'ancien chemin.
        Path dir = storage.findRunOutputDir(runId.toString())
                .orElseGet(() -> storage.outputDir(projectId == null ? null : projectId.toString(), runId.toString()));
        repository.deleteByRun(runId);

        if (!Files.isDirectory(dir)) {
            log.info("Aucune sortie à ingérer pour run={} (répertoire absent: {})", runId, dir);
            return 0;
        }

        List<OutputArtifact> artifacts = new ArrayList<>();
        List<ResultValue> values = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path f : walk.filter(Files::isRegularFile).toList()) {
                try {
                    String name = f.getFileName().toString();
                    ArtifactType type = ArtifactType.fromFileName(name);
                    long size = Files.size(f);
                    String rel = storage.relativize(f);
                    artifacts.add(OutputArtifact.create(runId, name, type, contentType(name), rel, size));

                    if (type == ArtifactType.CSV) {
                        values.addAll(parseCsvStreaming(runId, f));
                    }
                } catch (Exception e) {
                    log.warn("Fichier de sortie ignoré ({}): {}", f, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Échec du scan des sorties pour run={}: {}", runId, e.getMessage());
        }

        repository.saveArtifacts(artifacts);
        repository.saveValues(values);
        log.info("Ingestion run={} : {} artefact(s), {} valeur(s)", runId, artifacts.size(), values.size());
        return values.size();
    }

    /** Lit le CSV en flux (UTF-8, repli ISO-8859-1) sans charger tout le fichier en mémoire. */
    private static List<ResultValue> parseCsvStreaming(UUID runId, Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return CsvSeriesParser.parse(runId, r);
        } catch (MalformedInputException mie) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) {
                return CsvSeriesParser.parse(runId, r);
            }
        }
    }

    private static String contentType(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".svg")) return "image/svg+xml";
        if (n.endsWith(".csv")) return "text/csv";
        if (n.endsWith(".xml")) return "application/xml";
        return "application/octet-stream";
    }
}
