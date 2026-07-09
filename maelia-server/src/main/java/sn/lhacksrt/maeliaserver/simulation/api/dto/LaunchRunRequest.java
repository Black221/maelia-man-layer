package sn.lhacksrt.maeliaserver.simulation.api.dto;

import java.util.Map;

public record LaunchRunRequest(
        String modelPath,
        String experimentName,
        /** Condition d'arrêt GAML (ex. « sim_termine »). Null/absent = défaut de configuration. */
        String until,
        /** Valeurs de paramètres de l'expérience (clé = nom de variable GAML). Null = défauts du modèle. */
        Map<String, Object> parameters
) {}
