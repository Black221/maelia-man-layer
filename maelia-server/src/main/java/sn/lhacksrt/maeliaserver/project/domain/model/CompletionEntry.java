package sn.lhacksrt.maeliaserver.project.domain.model;

/** Une ligne du tableau de complétude : un DataSpec attendu et son état de remplissage. */
public record CompletionEntry(
        String dataSpecId,
        String module,
        String fileName,
        String fileType,
        String saisieMode,
        String generation,     // AUTO (généré par le module de prétraitement) | MANUAL (à fournir)
        boolean required,      // obligatoire vs optionnel
        String description,
        boolean datasetExists,
        String datasetStatus   // null si pas de dataset
) {}
