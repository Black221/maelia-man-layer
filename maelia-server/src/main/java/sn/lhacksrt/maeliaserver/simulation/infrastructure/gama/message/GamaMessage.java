package sn.lhacksrt.maeliaserver.simulation.infrastructure.gama.message;

/**
 * Représentation typée des messages du protocole GAMA WebSocket.
 * Voir architecture-backend.md §7.2 pour la liste exhaustive.
 *
 * Usage avec pattern matching (Java 21) :
 *   switch (msg) {
 *     case GamaMessage.CommandExecuted e -> handleLoad(e.expId());
 *     case GamaMessage.StatusInform s    -> pushProgress(s.cycle());
 *     case GamaMessage.SimulationEnded e -> handleEnd(e.expId());
 *     default -> log.debug("GAMA: {}", msg);
 *   }
 */
public sealed interface GamaMessage permits
        GamaMessage.ConnectionSuccessful,
        GamaMessage.CommandExecuted,
        GamaMessage.StatusInform,
        GamaMessage.StatusError,
        GamaMessage.StatusNeutral,
        GamaMessage.SimulationOutput,
        GamaMessage.SimulationDebug,
        GamaMessage.SimulationEnded,
        GamaMessage.SimulationError,
        GamaMessage.RuntimeError,
        GamaMessage.GamaServerError,
        GamaMessage.UnableToExecute,
        GamaMessage.MalformedRequest,
        GamaMessage.Unknown {

    /** Reçu à la connexion : GAMA est prêt à recevoir des commandes. */
    record ConnectionSuccessful(String content) implements GamaMessage {}

    /**
     * Réponse à "load" ou "play" : commande acceptée.
     * Pour "load", contient l'experiment_id.
     */
    record CommandExecuted(String expId, String content) implements GamaMessage {}

    /** Progression de la simulation (cycle, time, contenu). */
    record StatusInform(String expId, int cycle, double time, String content) implements GamaMessage {}

    record StatusError(String expId, String content) implements GamaMessage {}

    record StatusNeutral(String expId, String content) implements GamaMessage {}

    /** Sortie d'une instruction `write` dans le modèle GAML. */
    record SimulationOutput(String expId, String content) implements GamaMessage {}

    record SimulationDebug(String expId, String content) implements GamaMessage {}

    /** La simulation a atteint la condition `until` ou a été arrêtée proprement. */
    record SimulationEnded(String expId) implements GamaMessage {}

    record SimulationError(String expId, String content) implements GamaMessage {}

    record RuntimeError(String expId, String content) implements GamaMessage {}

    record GamaServerError(String content) implements GamaMessage {}

    record UnableToExecute(String content) implements GamaMessage {}

    record MalformedRequest(String content) implements GamaMessage {}

    /** Message de type inconnu — conserve le JSON brut pour le debug. */
    record Unknown(String type, String raw) implements GamaMessage {}
}
