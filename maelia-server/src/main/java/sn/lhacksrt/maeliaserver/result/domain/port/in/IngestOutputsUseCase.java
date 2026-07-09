package sn.lhacksrt.maeliaserver.result.domain.port.in;

import java.util.UUID;

/** Ingestion des sorties d'un run terminé (appelée par le worker de simulation). */
public interface IngestOutputsUseCase {

    /**
     * Scanne le répertoire de sortie du run et persiste artefacts + valeurs d'indicateurs.
     * Idempotent : remplace les résultats existants du run. Ne lève pas si aucune sortie.
     *
     * @return nombre de valeurs d'indicateurs ingérées
     */
    int ingest(UUID runId, UUID projectId);
}
