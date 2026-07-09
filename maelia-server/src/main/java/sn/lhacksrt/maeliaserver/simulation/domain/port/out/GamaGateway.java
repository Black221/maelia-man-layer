package sn.lhacksrt.maeliaserver.simulation.domain.port.out;

import sn.lhacksrt.maeliaserver.simulation.domain.model.SimulationRun;

/**
 * Port de sortie : ouvre une connexion vers le serveur GAMA headless.
 * Masque le transport réel (WebSocket serveur ou batch Docker).
 */
public interface GamaGateway {

    /**
     * Établit une connexion vers GAMA et retourne une session active.
     * La connexion DOIT rester ouverte jusqu'à l'appel de session.close() :
     * si le client ferme le socket, GAMA détruit les simulations en cours.
     *
     * @param run le run à piloter (pour le logging et la corrélation)
     */
    GamaSession open(SimulationRun run) throws Exception;
}
