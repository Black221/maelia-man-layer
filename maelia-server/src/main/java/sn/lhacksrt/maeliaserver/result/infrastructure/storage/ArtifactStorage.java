package sn.lhacksrt.maeliaserver.result.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Résout et lit les artefacts de sortie sur le volume gama-workspace.
 * Les chemins persistés sont relatifs à la racine du workspace.
 */
@Component
public class ArtifactStorage {

    @Value("${gama.workspace:./gama-workspace}")
    private String gamaWorkspace;

    /** Répertoire de sortie d'un run (fallback) : gama-workspace/maelia/projects/{projectId}/outputs/{runId}. */
    public Path outputDir(String projectId, String runId) {
        if (projectId != null) {
            return Paths.get(gamaWorkspace, "maelia", "projects", projectId, "outputs", runId);
        }
        return Paths.get(gamaWorkspace, "outputs", runId);
    }

    /**
     * Localise le répertoire de sortie réellement écrit par MAELIA :
     * {workspace}/maelia/models/main/log/&lt;territoire&gt;_&lt;nomSimulation&gt;_&lt;horodatage&gt;/{idSimulationAPI}.
     * Le seul segment fiable est le sous-dossier nommé {runId} (= idSimulationAPI).
     */
    public Optional<Path> findRunOutputDir(String runId) {
        Path logRoot = Paths.get(gamaWorkspace, "maelia", "models", "main", "log");
        if (!Files.isDirectory(logRoot)) return Optional.empty();
        try (Stream<Path> walk = Files.walk(logRoot, 3)) {
            return walk.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().equals(runId))
                    .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public Path workspaceRoot() {
        return Paths.get(gamaWorkspace).toAbsolutePath().normalize();
    }

    /** Chemin relatif (depuis la racine workspace) d'un fichier absolu. */
    public String relativize(Path file) {
        return workspaceRoot().relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    /** Lit les octets d'un artefact, en empêchant toute évasion hors du workspace. */
    public byte[] read(String relativePath) throws IOException {
        Path root = workspaceRoot();
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new SecurityException("Chemin d'artefact hors du workspace: " + relativePath);
        }
        return Files.readAllBytes(resolved);
    }
}
