package sn.lhacksrt.maeliaserver.preprocessing.domain.model;

import java.util.List;

/**
 * Entrée du plan de prétraitement d'un projet : un fichier applicable, son niveau
 * topologique, ses dépendances et l'état de satisfaction de celles-ci au regard
 * des datasets réellement présents dans le projet.
 */
public record GenerationPlanEntry(
        String dataSpecId,
        String module,
        String fileName,
        String generation,
        int level,
        List<String> dependencies,
        List<String> missingDependencies,
        boolean datasetExists,
        PlanEntryStatus status
) {}
