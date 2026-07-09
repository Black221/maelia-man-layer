package sn.lhacksrt.maeliaserver.simulation.infrastructure.gama;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    GamaServerSession(GamaWebSocketHandler handler, GamaMessageParser parser) {
        this.handler = handler;
        this.parser = parser;
        handler.setMessageListener(this::handleRaw);
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
    public void close() {
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
            // console, et la fin normale écrit "FIN DE SIMULATION". Sans cette détection, un run
            // cassé bloque waitForEnd (donc le consommateur RabbitMQ) jusqu'au timeout.
            case GamaMessage.SimulationOutput so -> {
                String c = so.content();
                if (c != null && c.contains("ERREUR LORS DE L'INITIALISATION")) {
                    log.error("GAMA: erreur d'initialisation détectée -> abandon du run");
                    endError.compareAndSet(null,
                            "Erreur lors de l'initialisation du modèle (fichier ou attribut manquant) — voir le journal GAMA");
                    endLatch.countDown();
                } else if (c != null && c.contains("FIN DE SIMULATION")) {
                    log.info("GAMA: 'FIN DE SIMULATION' détectée (filet de sécurité d'arrêt)");
                    endLatch.countDown();
                }
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
}
