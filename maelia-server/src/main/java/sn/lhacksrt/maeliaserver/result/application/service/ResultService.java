package sn.lhacksrt.maeliaserver.result.application.service;

import org.springframework.stereotype.Service;
import sn.lhacksrt.maeliaserver.result.domain.model.OutputArtifact;
import sn.lhacksrt.maeliaserver.result.domain.model.ResultValue;
import sn.lhacksrt.maeliaserver.result.domain.port.in.ResultQueryUseCase;
import sn.lhacksrt.maeliaserver.result.domain.port.out.ResultRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ResultService implements ResultQueryUseCase {

    private final ResultRepository repository;

    public ResultService(ResultRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ResultValue> values(UUID runId) {
        return repository.findValuesByRun(runId);
    }

    @Override
    public List<OutputArtifact> artifacts(UUID runId) {
        return repository.findArtifactsByRun(runId);
    }

    @Override
    public Optional<OutputArtifact> artifact(UUID artifactId) {
        return repository.findArtifact(artifactId);
    }
}
