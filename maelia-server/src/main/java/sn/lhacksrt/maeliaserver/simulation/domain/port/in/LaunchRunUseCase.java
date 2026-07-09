package sn.lhacksrt.maeliaserver.simulation.domain.port.in;

import sn.lhacksrt.maeliaserver.simulation.domain.model.SimulationRun;

import java.util.Map;
import java.util.UUID;

public interface LaunchRunUseCase {

    /**
     * Run de dev/test (M1) : modelPath, experimentName et condition d'arrêt fournis directement.
     *
     * @param until      expression GAML évaluée par le serveur GAMA après chaque pas pour
     *                   détecter la fin (ex. « sim_termine »). Vide = joue jusqu'à l'arrêt manuel.
     * @param parameters valeurs de paramètres de l'expérience (clé = nom de la variable GAML),
     *                   appliquées au « load ». Null/vide = défauts du modèle.
     */
    SimulationRun launch(String modelPath, String experimentName, String until,
                         Map<String, Object> parameters);

    /** Run projet (M4) : déduit modelPath/params depuis le projet et le scénario. */
    SimulationRun launchForProject(UUID projectId, UUID scenarioId);
}
