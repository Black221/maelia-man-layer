package sn.lhacksrt.maeliaserver.project.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.lhacksrt.maeliaserver.catalog.application.service.RequiredIfEvaluator;
import sn.lhacksrt.maeliaserver.catalog.domain.model.DataSpec;
import sn.lhacksrt.maeliaserver.catalog.domain.port.in.CatalogUseCase;
import sn.lhacksrt.maeliaserver.dataset.domain.port.in.DatasetQueryPort;
import sn.lhacksrt.maeliaserver.project.domain.model.CompletionEntry;
import sn.lhacksrt.maeliaserver.project.domain.model.ModelingConfiguration;
import sn.lhacksrt.maeliaserver.project.domain.model.Project;
import sn.lhacksrt.maeliaserver.project.domain.port.out.ProjectRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CatalogUseCase catalog;
    private final RequiredIfEvaluator evaluator;
    private final DatasetQueryPort datasetQuery;

    public ProjectService(ProjectRepository projectRepository,
                          CatalogUseCase catalog,
                          RequiredIfEvaluator evaluator,
                          DatasetQueryPort datasetQuery) {
        this.projectRepository = projectRepository;
        this.catalog = catalog;
        this.evaluator = evaluator;
        this.datasetQuery = datasetQuery;
    }

    public Project create(String name, String description) {
        Project project = Project.create(name, description);
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public List<Project> listAll() {
        return projectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Project> findById(UUID id) {
        return projectRepository.findById(id);
    }

    public Project updateName(UUID id, String name) {
        Project project = getOrThrow(id);
        project.updateName(name);
        return projectRepository.save(project);
    }

    /** Édition des informations générales (page Initialisation). */
    public Project updateInfo(UUID id, String name, String description) {
        Project project = getOrThrow(id);
        project.updateName(name);
        project.updateDescription(description);
        return projectRepository.save(project);
    }

    public Project updateModelingConfiguration(UUID id, ModelingConfiguration config) {
        Project project = getOrThrow(id);
        project.updateModelingConfiguration(config);
        return projectRepository.save(project);
    }

    public void archive(UUID id) {
        Project project = getOrThrow(id);
        project.archive();
        projectRepository.save(project);
    }

    /**
     * Tableau de complétude : DataSpec applicables (selon la config) croisés avec
     * les datasets réellement saisis du projet (existence + statut).
     */
    @Transactional(readOnly = true)
    public List<CompletionEntry> getCompletion(UUID projectId) {
        Project project = getOrThrow(projectId);
        Map<String, Object> cfgMap = project.getModelingConfiguration().toMap();

        List<DataSpec> applicable = catalog.getApplicableDataSpecs(cfgMap);

        Map<String, DatasetQueryPort.DatasetView> bySpec = datasetQuery.findByProject(projectId).stream()
                .collect(Collectors.toMap(DatasetQueryPort.DatasetView::dataSpecId,
                        Function.identity(), (a, b) -> a));

        return applicable.stream()
                .map(ds -> {
                    DatasetQueryPort.DatasetView v = bySpec.get(ds.id());
                    return new CompletionEntry(
                            ds.id(), ds.module(), ds.fileName(), ds.fileType(), ds.saisieMode(),
                            ds.generation(), ds.required(), ds.description(),
                            v != null && v.recordCount() > 0,
                            v != null ? v.status() : null
                    );
                })
                .toList();
    }

    private Project getOrThrow(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + id));
    }
}
