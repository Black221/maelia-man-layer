package sn.lhacksrt.maeliaserver.result.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import sn.lhacksrt.maeliaserver.result.domain.model.OutputArtifact;
import sn.lhacksrt.maeliaserver.result.domain.model.ResultValue;
import sn.lhacksrt.maeliaserver.result.domain.port.out.ResultRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ResultRepositoryAdapter implements ResultRepository {

    private final OutputArtifactJpaRepository artifactRepo;
    private final ResultValueJpaRepository valueRepo;

    public ResultRepositoryAdapter(OutputArtifactJpaRepository artifactRepo,
                                   ResultValueJpaRepository valueRepo) {
        this.artifactRepo = artifactRepo;
        this.valueRepo = valueRepo;
    }

    @Override
    @Transactional
    public void saveArtifacts(List<OutputArtifact> artifacts) {
        artifactRepo.saveAll(artifacts.stream().map(this::toEntity).toList());
    }

    @Override
    @Transactional
    public void saveValues(List<ResultValue> values) {
        valueRepo.saveAll(values.stream().map(this::toEntity).toList());
    }

    @Override
    public List<OutputArtifact> findArtifactsByRun(UUID runId) {
        return artifactRepo.findByRunIdOrderByName(runId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<OutputArtifact> findArtifact(UUID artifactId) {
        return artifactRepo.findById(artifactId).map(this::toDomain);
    }

    @Override
    public List<ResultValue> findValuesByRun(UUID runId) {
        return valueRepo.findByRunIdOrderByIndicatorAscObsDateAscCycleAsc(runId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteByRun(UUID runId) {
        artifactRepo.deleteByRunId(runId);
        valueRepo.deleteByRunId(runId);
    }

    private OutputArtifactJpaEntity toEntity(OutputArtifact a) {
        return new OutputArtifactJpaEntity(a.id(), a.runId(), a.name(), a.type(),
                a.contentType(), a.relativePath(), a.sizeBytes(), a.createdAt());
    }

    private OutputArtifact toDomain(OutputArtifactJpaEntity e) {
        return new OutputArtifact(e.getId(), e.getRunId(), e.getName(), e.getArtifactType(),
                e.getContentType(), e.getRelativePath(), e.getSizeBytes(), e.getCreatedAt());
    }

    private ResultValueJpaEntity toEntity(ResultValue v) {
        return new ResultValueJpaEntity(v.id(), v.runId(), v.indicator(), v.category(), v.zone(),
                v.date(), v.cycle(), v.year(), v.value(), v.unit());
    }

    private ResultValue toDomain(ResultValueJpaEntity e) {
        return new ResultValue(e.getId(), e.getRunId(), e.getIndicator(), e.getCategory(), e.getZone(),
                e.getObsDate(), e.getCycle(), e.getYear(), e.getValue(), e.getUnit());
    }
}
