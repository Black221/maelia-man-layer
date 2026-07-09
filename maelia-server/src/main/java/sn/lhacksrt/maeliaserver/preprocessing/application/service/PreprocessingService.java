package sn.lhacksrt.maeliaserver.preprocessing.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.lhacksrt.maeliaserver.catalog.domain.port.in.CatalogUseCase;
import sn.lhacksrt.maeliaserver.dataset.domain.port.in.DatasetQueryPort;
import sn.lhacksrt.maeliaserver.preprocessing.domain.model.DependencyGraph;
import sn.lhacksrt.maeliaserver.preprocessing.domain.model.DependencyNode;
import sn.lhacksrt.maeliaserver.preprocessing.domain.model.GenerationPlanEntry;
import sn.lhacksrt.maeliaserver.preprocessing.domain.model.PlanEntryStatus;
import sn.lhacksrt.maeliaserver.preprocessing.domain.port.in.PreprocessingUseCase;
import sn.lhacksrt.maeliaserver.preprocessing.domain.service.DependencyGraphBuilder;
import sn.lhacksrt.maeliaserver.project.domain.model.Project;
import sn.lhacksrt.maeliaserver.project.domain.port.out.ProjectRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PreprocessingService implements PreprocessingUseCase {

    private final CatalogUseCase catalog;
    private final ProjectRepository projectRepository;
    private final DatasetQueryPort datasetQuery;

    public PreprocessingService(CatalogUseCase catalog,
                                ProjectRepository projectRepository,
                                DatasetQueryPort datasetQuery) {
        this.catalog = catalog;
        this.projectRepository = projectRepository;
        this.datasetQuery = datasetQuery;
    }

    @Override
    public DependencyGraph getDependencyGraph() {
        return DependencyGraphBuilder.build(catalog.getAllDataSpecs());
    }

    @Override
    public List<GenerationPlanEntry> getGenerationPlan(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        Map<String, Object> cfgMap = project.getModelingConfiguration().toMap();

        // Graphe restreint aux fichiers applicables : les dépendances vers des fichiers
        // hors périmètre de la configuration sont ignorées (unknownReferences).
        DependencyGraph graph = DependencyGraphBuilder.build(catalog.getApplicableDataSpecs(cfgMap));

        Map<String, DatasetQueryPort.DatasetView> bySpec = datasetQuery.findByProject(projectId).stream()
                .collect(Collectors.toMap(DatasetQueryPort.DatasetView::dataSpecId,
                        Function.identity(), (a, b) -> a));

        return graph.nodes().stream()
                .map(node -> toPlanEntry(node, bySpec))
                .toList();
    }

    private GenerationPlanEntry toPlanEntry(DependencyNode node,
                                            Map<String, DatasetQueryPort.DatasetView> bySpec) {
        boolean exists = datasetExists(bySpec.get(node.dataSpecId()));
        List<String> missing = node.dependsOn().stream()
                .filter(dep -> !datasetExists(bySpec.get(dep)))
                .toList();
        PlanEntryStatus status = exists ? PlanEntryStatus.DONE
                : missing.isEmpty() ? PlanEntryStatus.READY
                : PlanEntryStatus.BLOCKED;
        return new GenerationPlanEntry(
                node.dataSpecId(), node.module(), node.fileName(), node.generation(),
                node.level(), node.dependsOn(), missing, exists, status);
    }

    /** Un dataset satisfait une dépendance s'il a des enregistrements ou est VALIDE (cas SHP uploadé). */
    private boolean datasetExists(DatasetQueryPort.DatasetView v) {
        return v != null && (v.recordCount() > 0 || "VALIDE".equalsIgnoreCase(v.status()));
    }
}
