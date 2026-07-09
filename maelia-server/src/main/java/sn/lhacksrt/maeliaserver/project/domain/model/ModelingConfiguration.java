package sn.lhacksrt.maeliaserver.project.domain.model;

import java.util.List;

/**
 * Configuration de modélisation d'un projet MAELIA.
 * Pilote l'évaluation des requiredIf du catalogue.
 */
public record ModelingConfiguration(
        String assolementMethod,       // DONNEES_ENTREE | FONCTIONS_DE_CROYANCE
        String irrigationMode,         // BLOC | SIMPLE
        String cropModel,              // AQYIELD | HERBSIM | SIMPLE
        String restrictionMethod,      // SIMPLE | COMPLEXE
        List<String> modules,          // agricole, hydrographique, normatif,
        String scenarioClimatique      // nullable
) {
    /**
     * Configuration de base d'un projet MAELIA (cas Garonne-Amont), dérivée des défauts
     * du launcher : assolement par données, modèle de culture AqYield (requis pour les
     * sorties eau/azote), irrigation simple, modules agricole + hydrographique.
     */
    public static ModelingConfiguration defaults() {
        return new ModelingConfiguration(
                "DONNEES_ENTREE", "SIMPLE", "AQYIELD", "SIMPLE",
                List.of("agricole", "hydrographique"), null
        );
    }

    public java.util.Map<String, Object> toMap() {
        var map = new java.util.LinkedHashMap<String, Object>();
        map.put("assolementMethod", assolementMethod);
        map.put("irrigationMode", irrigationMode);
        map.put("cropModel", cropModel);
        map.put("restrictionMethod", restrictionMethod);
        map.put("modules", modules);
        map.put("scenarioClimatique", scenarioClimatique);
        return map;
    }
}
