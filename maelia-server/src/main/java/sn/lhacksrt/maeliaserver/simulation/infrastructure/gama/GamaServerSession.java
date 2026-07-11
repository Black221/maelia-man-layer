package sn.lhacksrt.maeliaserver.simulation.infrastructure.gama;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import sn.lhacksrt.maeliaserver.simulation.domain.port.out.GamaSession;
import sn.lhacksrt.maeliaserver.simulation.infrastructure.gama.message.GamaMessage;
import sn.lhacksrt.maeliaserver.simulation.infrastructure.gama.message.GamaMessageParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class GamaServerSession implements GamaSession {

    private static final Logger log = LoggerFactory.getLogger(GamaServerSession.class);

    private final GamaWebSocketHandler handler;
    private final GamaMessageParser parser;
    private final ObjectMapper mapper = new ObjectMapper();

    private final CompletableFuture<String> loadFuture = new CompletableFuture<>();
    private final CountDownLatch endLatch = new CountDownLatch(1);
    private final AtomicReference<String> endError = new AtomicReference<>();

    private Consumer<String> externalListener;
    /** Vrai quand close() est appelé volontairement (fin normale) : la fermeture WS qui suit
     *  ne doit alors pas être interprétée comme un crash. */
    private volatile boolean deliberateClose = false;

    GamaServerSession(GamaWebSocketHandler handler, GamaMessageParser parser) {
        this.handler = handler;
        this.parser = parser;
        handler.setMessageListener(this::handleRaw);
        handler.setCloseListener(this::onConnectionClosed);
    }

    /**
     * Fermeture inattendue de la connexion WebSocket (crash / arrêt brutal du serveur GAMA)
     * pendant un run : on débloque immédiatement {@link #waitForEnd} avec une erreur, au lieu
     * d'attendre le timeout (qui immobiliserait un consommateur RabbitMQ jusqu'à 30 min).
     * No-op si la simulation est déjà terminée ou si close() a été appelé volontairement.
     */
    private void onConnectionClosed(CloseStatus status) {
        if (deliberateClose || endLatch.getCount() == 0) return;
        String reason = "Connexion GAMA fermée avant la fin de la simulation (code=" + status.getCode()
                + (status.getReason() != null && !status.getReason().isBlank() ? " " + status.getReason() : "")
                + ") — le serveur GAMA a probablement planté";
        log.error("GAMA: {}", reason);
        endError.compareAndSet(null, reason);
        // Débloque aussi un load() encore en attente (crash pendant l'initialisation).
        if (!loadFuture.isDone()) {
            loadFuture.completeExceptionally(new IllegalStateException(reason));
        }
        endLatch.countDown();
    }

    @Override
    public String load(String modelPath, String experimentName, String until,
                       List<Map<String, Object>> parameters) throws Exception {
        Map<String, Object> command = new HashMap<>();
        command.put("type", "load");
        command.put("model", modelPath);
        command.put("experiment", experimentName);
        command.put("until", until != null ? until : "");
        command.put("parameters", parameters != null ? parameters.toArray() : new Object[0]);
        command.put("status", true);
        command.put("console", true);
        command.put("runtime", true);

        String payload = mapper.writeValueAsString(command);
        handler.sendCommand(payload);
        // Payload complet : permet de comparer mot à mot ce qui est envoyé par le run de test
        // (/dev/runs/maelia-test) et par un run de projet quand l'un marche et pas l'autre.
        log.info("GAMA load sent: {}", payload);

        return loadFuture.get(60, TimeUnit.SECONDS);
    }

    @Override
    public void play(String experimentId) throws Exception {
        Map<String, Object> command = Map.of(
                "type", "play",
                "exp_id", experimentId,
                "sync", false
        );
        handler.sendCommand(mapper.writeValueAsString(command));
        log.info("GAMA play sent: exp_id={}", experimentId);
    }

    @Override
    public void stop(String experimentId) throws Exception {
        if (experimentId == null || experimentId.isBlank()) return;
        Map<String, Object> command = Map.of(
                "type", "stop",
                "exp_id", experimentId
        );
        handler.sendCommand(mapper.writeValueAsString(command));
        log.info("GAMA stop sent: exp_id={}", experimentId);
    }

    @Override
    public String evaluate(String experimentId, String gamlExpression) throws Exception {
        CompletableFuture<String> evalFuture = new CompletableFuture<>();
        Map<String, Object> command = Map.of(
                "type", "expression",
                "exp_id", experimentId,
                "expr", gamlExpression
        );
        Consumer<String> prevListener = externalListener;
        handler.setMessageListener(raw -> {
            GamaMessage msg = parser.parse(raw);
            if (msg instanceof GamaMessage.CommandExecuted ce) {
                evalFuture.complete(ce.content());
                handler.setMessageListener(prevListener != null ? prevListener::accept : this::handleRaw);
            } else {
                handleRaw(raw);
            }
        });
        handler.sendCommand(mapper.writeValueAsString(command));
        return evalFuture.get(30, TimeUnit.SECONDS);
    }

    @Override
    public void onMessage(Consumer<String> listener) {
        this.externalListener = listener;
    }

    @Override
    public void waitForEnd(long timeoutSeconds) throws Exception {
        boolean ended = endLatch.await(timeoutSeconds, TimeUnit.SECONDS);
        if (!ended) {
            throw new IllegalStateException("GAMA simulation timed out after " + timeoutSeconds + "s");
        }
        String error = endError.get();
        if (error != null) {
            throw new IllegalStateException("GAMA simulation failed: " + error);
        }
    }

    @Override
    public boolean awaitEnd(long timeoutSeconds) throws Exception {
        boolean ended = endLatch.await(timeoutSeconds, TimeUnit.SECONDS);
        String error = endError.get();
        if (error != null) {
            throw new IllegalStateException("GAMA simulation failed: " + error);
        }
        return ended;
    }

    @Override
    public void close() {
        deliberateClose = true; // la fermeture WS qui suit est volontaire, pas un crash
        try {
            var session = handler.getSession();
            if (session != null && session.isOpen()) {
                session.close();
                log.info("GAMA WebSocket session closed");
            }
        } catch (Exception e) {
            log.warn("Error closing GAMA session: {}", e.getMessage());
        }
    }

    private void handleRaw(String raw) {
        GamaMessage msg = parser.parse(raw);

        switch (msg) {
            case GamaMessage.ConnectionSuccessful c ->
                    log.info("GAMA connection confirmed: {}", c.content());

            case GamaMessage.CommandExecuted ce -> {
                if (!loadFuture.isDone()) {
                    log.info("GAMA experiment loaded, exp_id={}", ce.expId());
                    loadFuture.complete(ce.expId());
                }
            }

            // TRACE : GAMA émet ces statuts en continu (plusieurs milliers/s pendant l'init
            // MAELIA) — en DEBUG ils noient les logs du worker.
            case GamaMessage.StatusInform si ->
                    log.trace("GAMA cycle={}", (Object) Optional.of(si.cycle()));

            // Les expériences MAELIA (type gui) ne signalent PAS toujours la fin/erreur par un
            // message structuré : une erreur d'init écrit "ERREUR LORS DE L'INITIALISATION" sur la
            // console, la fin normale écrit "FIN DE SIMULATION", et une erreur d'EXÉCUTION (ex.
            // "Division by zero" dans un reflex) est déversée sur la console (GamaRuntimeException)
            // SANS message structuré. Sans cette détection, un run cassé en cours de route bloque
            // waitForEnd (donc le consommateur RabbitMQ) jusqu'au timeout, et le run reste EN_COURS.
            case GamaMessage.SimulationOutput so -> {
                String c = so.content();
                if (c != null && c.contains("ERREUR LORS DE L'INITIALISATION")) {
                    log.error("GAMA: erreur d'initialisation détectée -> abandon du run");
                    endError.compareAndSet(null,
                            "Erreur lors de l'initialisation du modèle (fichier ou attribut manquant) — voir le journal GAMA");
                    endLatch.countDown();
                } else if (isRuntimeErrorConsole(c)) {
                    log.error("GAMA: erreur d'exécution détectée sur la console -> abandon du run : {}", firstLine(c));
                    endError.compareAndSet(null,
                            "Erreur d'exécution du modèle GAMA : " + firstLine(c) + " — voir le journal GAMA");
                    endLatch.countDown();
                } else if (c != null && c.contains("FIN DE SIMULATION")) {
                    log.info("GAMA: 'FIN DE SIMULATION' détectée (filet de sécurité d'arrêt)");
                    endLatch.countDown();
                }
            }

            // Statut d'expérience passé en ERREUR (SimulationStatusError) : erreur d'exécution
            // signalée de façon structurée par le serveur GAMA -> fatal.
            case GamaMessage.StatusError se -> {
                log.error("GAMA SimulationStatusError: {}", se.content());
                endError.compareAndSet(null, "Erreur d'exécution du modèle GAMA : "
                        + (se.content() != null && !se.content().isBlank() ? se.content() : "statut ERREUR")
                        + " — voir le journal GAMA");
                endLatch.countDown();
            }

            case GamaMessage.SimulationEnded se -> {
                log.info("GAMA SimulationEnded exp_id={}", se.expId());
                endLatch.countDown();
            }

            case GamaMessage.SimulationError se -> {
                log.error("GAMA SimulationError: {}", se.content());
                endError.set(se.content());
                endLatch.countDown();
            }

            case GamaMessage.RuntimeError re -> {
                log.error("GAMA RuntimeError: {}", re.content());
                endError.set(re.content());
                endLatch.countDown();
            }

            case GamaMessage.GamaServerError ge -> {
                log.error("GAMA ServerError: {}", ge.content());
                endError.set(ge.content());
                endLatch.countDown();
            }

            default -> log.debug("GAMA: {}", raw);
        }

        if (externalListener != null) {
            externalListener.accept(raw);
        }
    }

    /**
     * Détecte une erreur d'EXÉCUTION GAMA déversée sur la console (sans message structuré).
     * Marqueurs robustes et non ambigus : la classe d'exception GAMA (présente dans toute trace)
     * et les formulations d'erreur runtime. Volontairement conservateur pour ne pas confondre
     * un simple {@code write} du modèle avec une vraie erreur.
     */
    private static boolean isRuntimeErrorConsole(String c) {
        if (c == null || c.isBlank()) return false;
        String low = c.toLowerCase();
        return low.contains("gamaruntimeexception")
                || low.contains("gama.core.runtime.exceptions")
                || low.contains("runtime error");
    }

    /** 1re ligne non vide d'un contenu multi-lignes (pour un message d'erreur lisible). */
    private static String firstLine(String c) {
        if (c == null) return "";
        for (String line : c.split("\\R")) {
            if (!line.isBlank()) return line.trim();
        }
        return c.trim();
    }
}
