package sn.lhacksrt.maeliaserver.preprocessing.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.lhacksrt.maeliaserver.preprocessing.api.dto.DependencyGraphDto;
import sn.lhacksrt.maeliaserver.preprocessing.api.dto.GenerationPlanEntryDto;
import sn.lhacksrt.maeliaserver.preprocessing.domain.port.in.PreprocessingUseCase;

import java.util.List;
import java.util.UUID;

/**
 * Module de prétraitement : graphe de dépendances du catalogue et plan de
 * génération ordonné par projet. La génération effective des fichiers (SIG)
 * sera branchée ultérieurement sur ce plan.
 */
@RestController
@RequestMapping("/api/v1")
public class PreprocessingController {

    private final PreprocessingUseCase preprocessing;

    public PreprocessingController(PreprocessingUseCase preprocessing) {
        this.preprocessing = preprocessing;
    }

    /** Graphe global des dépendances entre les fichiers du catalogue. */
    @GetMapping("/preprocessing/dependency-graph")
    public DependencyGraphDto dependencyGraph() {
        return DependencyGraphDto.from(preprocessing.getDependencyGraph());
    }

    /** Plan de prétraitement du projet, trié par niveau topologique. */
    @GetMapping("/projects/{projectId}/preprocessing/plan")
    public List<GenerationPlanEntryDto> plan(@PathVariable UUID projectId) {
        return preprocessing.getGenerationPlan(projectId).stream()
                .map(GenerationPlanEntryDto::from)
                .toList();
    }
}
