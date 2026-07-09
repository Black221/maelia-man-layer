package sn.lhacksrt.maeliaserver.project.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import sn.lhacksrt.maeliaserver.project.domain.model.ModelingConfiguration;
import sn.lhacksrt.maeliaserver.project.domain.model.Project;
import sn.lhacksrt.maeliaserver.project.domain.model.ProjectStatus;
import sn.lhacksrt.maeliaserver.project.domain.port.out.ProjectRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProjectRepositoryAdapter implements ProjectRepository {

    private final ProjectJpaRepository jpa;
    private final ObjectMapper objectMapper;

    public ProjectRepositoryAdapter(ProjectJpaRepository jpa, ObjectMapper objectMapper) {
        this.jpa = jpa;
        this.objectMapper = objectMapper;
    }

    @Override
    public Project save(Project project) {
        ProjectJpaEntity entity = jpa.findById(project.getId())
                .orElse(new ProjectJpaEntity());
        entity.setId(project.getId());
        entity.setName(project.getName());
        entity.setDescription(project.getDescription());
        entity.setStudyArea(project.getStudyArea());
        entity.setModelingConfiguration(project.getModelingConfiguration().toMap());
        entity.setStatus(project.getStatus().name());
        entity.setCreatedAt(project.getCreatedAt());
        entity.setUpdatedAt(project.getUpdatedAt());
        jpa.save(entity);
        return project;
    }

    @Override
    public Optional<Project> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Project> findAll() {
        return jpa.findAllActive().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }

    @SuppressWarnings("unchecked")
    private Project toDomain(ProjectJpaEntity e) {
        Map<String, Object> cfg = e.getModelingConfiguration() != null
                ? e.getModelingConfiguration()
                : Map.of();

        ModelingConfiguration config = new ModelingConfiguration(
                (String) cfg.getOrDefault("assolementMethod", "DONNEES_ENTREE"),
                (String) cfg.getOrDefault("irrigationMode", "SIMPLE"),
                (String) cfg.getOrDefault("cropModel", "SIMPLE"),
                (String) cfg.getOrDefault("restrictionMethod", "SIMPLE"),
                cfg.containsKey("modules") ? (List<String>) cfg.get("modules") : List.of("agricole", "hydrographique"),
                (String) cfg.get("scenarioClimatique")
        );

        return Project.reconstitute(
                e.getId(), e.getName(), e.getDescription(), e.getStudyArea(),
                config, ProjectStatus.valueOf(e.getStatus()),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
