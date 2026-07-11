package sn.lhacksrt.maeliaserver.result.application.ingestion;

import sn.lhacksrt.maeliaserver.result.domain.model.ResultValue;

import java.io.Reader;
import java.util.List;
import java.util.UUID;

/**
 * Ingestor spécialisé d'un (ou plusieurs) fichier(s) de sortie MAELIA connu(s).
 *
 * Contrairement au parseur générique {@link CsvSeriesParser} (fallback), un ingestor connaît la
 * structure du fichier : quelles colonnes sont des dimensions (année, culture, parcelle) et
 * quelles colonnes sont des indicateurs — ce qui permet d'agréger « par culture au cours du temps ».
 */
public interface OutputFileIngestor {

    /** Vrai si cet ingestor sait traiter ce nom de fichier. */
    boolean supports(String fileName);

    /** Extrait les valeurs d'indicateurs (flux, best-effort). Le nom de fichier lève l'ambiguïté. */
    List<ResultValue> parse(UUID runId, Reader reader, String fileName);
}
