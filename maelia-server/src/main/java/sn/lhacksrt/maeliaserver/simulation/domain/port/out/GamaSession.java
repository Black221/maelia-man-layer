package sn.lhacksrt.maeliaserver.simulation.domain.port.out;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Port de sortie : session active vers GAMA.
 * Une session = une connexion WebSocket maintenue ouverte pendant tout le run.
 */
public interface GamaSession extends AutoCloseable {

    /**
     * Envoie la commande "load" avec des paramètres optionnels et attend l'experiment_id.
     *
     * @param modelPath      chemin absolu du .gaml dans le volume GAMA
     * @param experimentName nom de l'expérience GAML
     * @param until          expression GAML de condition d'arrêt (vide = fin naturelle)
     * @param parameters     paramètres GAMA ({type, name, value}) — liste vide pour les valeurs par défaut
     * @return l'experiment_id retourné par GAMA
     */
    String load(String modelPath, String experimentName, String until,
                List<Map<String, Object>> parameters) throws Exception;

    /** Envoie la commande "play". */
    void play(String experimentId) throws Exception;

    /** Envoie la commande "stop" : arrête l'expérience côté GAMA (annulation / nettoyage). */
    void stop(String experimentId) throws Exception;

    /** Évalue une expression GAML et retourne la valeur brute. */
    String evaluate(String experimentId, String gamlExpression) throws Exception;

    /** Enregistre un listener qui reçoit chaque message GAMA brut. */
    void onMessage(Consumer<String> listener);

    /** Bloque jusqu'à la fin de la simulation ou le timeout. */
    void waitForEnd(long timeoutSeconds) throws Exception;
}
