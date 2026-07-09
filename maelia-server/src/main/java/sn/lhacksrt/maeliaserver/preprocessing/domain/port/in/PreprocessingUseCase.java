package sn.lhacksrt.maeliaserver.preprocessing.domain.port.in;

import sn.lhacksrt.maeliaserver.preprocessing.domain.model.DependencyGraph;
import sn.lhacksrt.maeliaserver.preprocessing.domain.model.GenerationPlanEntry;

import java.util.List;
import java.util.UUID;

/**
 * Cas d'usage du module de prétraitement : exposer le graphe de dépendances
 * du catalogue et le plan de génération ordonné d'un projet.
 */
public interface PreprocessingUseCase {

    /** Graphe global des dépendances entre tous les fichiers du catalogue. */
    DependencyGraph getDependencyGraph();

    /**
     * Plan de prétraitement d'un projet : fichiers applicables (selon la config de
     * modélisation) triés par niveau topologique, avec l'état de leurs dépendances
     * au regard des datasets présents.
     */
    List<GenerationPlanEntry> getGenerationPlan(UUID projectId);
}
