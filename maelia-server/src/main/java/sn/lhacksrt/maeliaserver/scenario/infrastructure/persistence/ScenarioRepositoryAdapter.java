package sn.lhacksrt.maeliaserver.scenario.infrastructure.persistence;

import org.springframework.stereotype.Component;
import sn.lhacksrt.maeliaserver.scenario.domain.model.Scenario;
import sn.lhacksrt.maeliaserver.scenario.domain.port.out.ScenarioRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ScenarioRepositoryAdapter implements ScenarioRepository {

    private final ScenarioJpaRepository jpa;

    public ScenarioRepositoryAdapter(ScenarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Scenario save(Scenario scenario) {
        ScenarioJpaEntity entity = jpa.findById(scenario.getId())
                .orElseGet(() -> new ScenarioJpaEntity(
                        scenario.getId(), scenario.getProjectId(), scenario.getName(),
                        scenario.getDescription(), scenario.getParameterValues(),
                        scenario.getCreatedAt()));

        entity.setName(scenario.getName());
        entity.setDescription(scenario.getDescription());
        entity.setParameterValues(scenario.getParameterValues());
        entity.setArchivedAt(scenario.getArchivedAt());

        jpa.save(entity);
        return scenario;
    }

    @Override
    public Optional<Scenario> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Scenario> findByProject(UUID projectId) {
        return jpa.findByProjectIdAndArchivedAtIsNullOrderByCreatedAtDesc(projectId)
                .stream().map(this::toDomain).toList();
    }

    private Scenario toDomain(ScenarioJpaEntity e) {
        return Scenario.reconstitute(
                e.getId(), e.getProjectId(), e.getName(), e.getDescription(),
                e.getParameterValues(), e.getCreatedAt(), e.getArchivedAt());
    }
}
