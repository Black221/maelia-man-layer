package sn.lhacksrt.maeliaserver.result.domain.port.out;

import sn.lhacksrt.maeliaserver.result.domain.model.OutputArtifact;
import sn.lhacksrt.maeliaserver.result.domain.model.ResultValue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port de persistance des résultats (sorties d'un run). */
public interface ResultRepository {

    void saveArtifacts(List<OutputArtifact> artifacts);

    void saveValues(List<ResultValue> values);

    List<OutputArtifact> findArtifactsByRun(UUID runId);

    Optional<OutputArtifact> findArtifact(UUID artifactId);

    List<ResultValue> findValuesByRun(UUID runId);

    /** Supprime les résultats existants d'un run (ingestion idempotente). */
    void deleteByRun(UUID runId);
}
