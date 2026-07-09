package sn.lhacksrt.maeliaserver.result.domain.port.in;

import sn.lhacksrt.maeliaserver.result.domain.model.OutputArtifact;
import sn.lhacksrt.maeliaserver.result.domain.model.ResultValue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Lecture des résultats d'un run pour restitution. */
public interface ResultQueryUseCase {

    List<ResultValue> values(UUID runId);

    List<OutputArtifact> artifacts(UUID runId);

    Optional<OutputArtifact> artifact(UUID artifactId);
}
