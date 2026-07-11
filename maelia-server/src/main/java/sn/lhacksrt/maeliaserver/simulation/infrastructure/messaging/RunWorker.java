package sn.lhacksrt.maeliaserver.simulation.infrastructure.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import sn.lhacksrt.maeliaserver.result.domain.port.in.IngestOutputsUseCase;
import sn.lhacksrt.maeliaserver.simulation.domain.model.SimulationRun;
import sn.lhacksrt.maeliaserver.simulation.domain.port.out.GamaGateway;
import sn.lhacksrt.maeliaserver.simulation.domain.port.out.GamaSession;
import sn.lhacksrt.maeliaserver.simulation.domain.port.out.SimulationRunRepository;
import sn.lhacksrt.maeliaserver.simulation.infrastructure.gama.message.GamaMessage;
import sn.lhacksrt.maeliaserver.simulation.infrastructure.gama.message.GamaMessageParser;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Consomme la file des runs (profil worker), pilote GAMA et publie la progression
 * sur l'échange fanout des mises à jour (relayée vers STOMP par l'API).
 */
@Component
@Profile("worker")
public class RunWorker {

    private static final Logger log = LoggerFactory.getLogger(RunWorker.class);

    private final SimulationRunRepository repository;
    private final GamaGateway gamaGateway;
    private final GamaMessageParser messageParser;
    private final RabbitTemplate rabbitTemplate;
    private final IngestOutputsUseCase ingestOutputs;
    private final MeterRegistry meterRegistry;

    /** Délai max d'une SIMULATION complète (distinct du command-timeout court). */
    @Value("${gama.run-timeout-seconds:7200}")
    private long runTimeoutSeconds;

    /** Intervalle de sondage de fin de simulation (variable simulationTerminee). */
    @Value("${gama.end-poll-seconds:10}")
    private long endPollSeconds;

    @Value("${maelia.messaging.run-updates-exchange:maelia.run.updates}")
    private String runUpdatesExchange;

    public RunWorker(SimulationRunRepository repository,
                     GamaGateway gamaGateway,
                     GamaMessageParser messageParser,
                     RabbitTemplate rabbitTemplate,
                     IngestOutputsUseCase ingestOutputs,
                     MeterRegistry meterRegistry) {
        this.repository = repository;
        this.gamaGateway = gamaGateway;
        this.messageParser = messageParser;
        this.rabbitTemplate = rabbitTemplate;
        this.ingestOutputs = ingestOutputs;
        this.meterRegistry = meterRegistry;
    }

    @RabbitListener(queues = "${maelia.messaging.run-queue}")
    public void consume(RunLaunchMessage msg) {
        UUID runId = msg.runId();
        MDC.put("runId", runId.toString());
        long startNanos = System.nanoTime();
        meterRegistry.counter("maelia.runs", "event", "received").increment();
        try {
            log.info("Worker received run={}", runId);

            SimulationRun run = repository.findById(runId)
                    .orElseThrow(() -> new IllegalStateException("Run not found: " + runId));
            if (run.getProjectId() != null) {
                MDC.put("projectId", run.getProjectId().toString());
            }

            try {
                executeRun(run, msg);
                meterRegistry.counter("maelia.runs", "event", "finished").increment();
            } catch (Exception ex) {
                log.error("Run {} failed: {}", runId, ex.getMessage(), ex);
                run.markFailed(ex.getMessage());
                repository.save(run);
                publish(runId, "ECHEC", 0, null, ex.getMessage());
                meterRegistry.counter("maelia.runs", "event", "failed").increment();
            }
        } finally {
            meterRegistry.timer("maelia.run.duration").record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
            MDC.clear();
        }
    }

    private void executeRun(SimulationRun run, RunLaunchMessage msg) throws Exception {
        UUID runId = run.getId();

        try (GamaSession session = gamaGateway.open(run)) {

            // GAMA émet les statuts en flot continu (milliers/s au même cycle pendant l'init) :
            // on ne republie PROGRESS que quand le cycle change, sinon RabbitMQ/STOMP saturent.
            java.util.concurrent.atomic.AtomicInteger lastCycle = new java.util.concurrent.atomic.AtomicInteger(-1);
            session.onMessage(raw -> forwardGamaMessage(runId, raw, lastCycle));

            List<java.util.Map<String, Object>> params =
                    msg.gamaParameters() != null ? msg.gamaParameters() : List.of();

            String expId = session.load(msg.modelPath(), msg.experimentName(),
                    msg.until() != null ? msg.until() : "", params);
            run.markStarted(expId);
            repository.save(run);
            publish(runId, "EN_COURS", 0, null, null);

            try {
                session.play(expId);
                awaitSimulationEnd(session, expId, runId);
            } catch (Exception ex) {
                // Nettoyage côté GAMA avant de propager (annulation/timeout)
                try { session.stop(expId); } catch (Exception ignored) {}
                throw ex;
            }

            int finalCycle = 0;
            try {
                String cycleStr = session.evaluate(expId, "cycle");
                finalCycle = Integer.parseInt(cycleStr.trim().replaceAll("[^0-9]", ""));
            } catch (Exception e) {
                log.warn("Could not evaluate final cycle for run={}: {}", runId, e.getMessage());
            }

            run.markFinished(finalCycle);
            repository.save(run);

            // M5 : ingestion des sorties (best-effort, n'échoue pas le run)
            try {
                int ingested = ingestOutputs.ingest(runId, run.getProjectId());
                meterRegistry.counter("maelia.result.values.ingested").increment(ingested);
                log.info("Run {} : {} valeur(s) de résultat ingérée(s)", runId, ingested);
            } catch (Exception e) {
                log.warn("Ingestion des sorties échouée pour run={}: {}", runId, e.getMessage());
            }

            publish(runId, "TERMINE", finalCycle, null, null);
            log.info("Run {} completed at cycle={}", runId, (Object) finalCycle);
        }
    }

    /**
     * Attend la fin de la simulation. GAMA peut terminer SANS émettre d'événement serveur : le
     * modèle MAELIA fait {@code do pause} en fin de run (l'expérience reste pausée mais vivante).
     * En plus d'écouter les signaux (console « FIN DE SIMULATION », messages de fin/erreur,
     * fermeture de connexion — via {@code awaitEnd}), on SONDE la variable globale
     * {@code simulationTerminee} du modèle, posée à {@code true} à la fin. Sans ce sondage, un run
     * pourtant terminé restait « EN_COURS » jusqu'au timeout.
     */
    private void awaitSimulationEnd(GamaSession session, String expId, UUID runId) throws Exception {
        long pollSeconds = Math.max(2, Math.min(endPollSeconds, runTimeoutSeconds));
        long deadlineNanos = System.nanoTime() + runTimeoutSeconds * 1_000_000_000L;
        while (true) {
            if (session.awaitEnd(pollSeconds)) return; // fin par signal (peut lever en cas d'erreur/crash)
            if (System.nanoTime() >= deadlineNanos) {
                throw new IllegalStateException("GAMA simulation timed out after " + runTimeoutSeconds + "s");
            }
            // Fallback robuste : sonder la variable de fin du modèle (best-effort).
            try {
                String v = session.evaluate(expId, "simulationTerminee");
                if (v != null && v.trim().toLowerCase().startsWith("true")) {
                    log.info("Run {} : fin détectée via simulationTerminee=true", runId);
                    return;
                }
            } catch (Exception e) {
                log.debug("Run {} : sondage simulationTerminee ignoré ({})", runId, e.getMessage());
            }
        }
    }

    private void forwardGamaMessage(UUID runId, String raw,
                                    java.util.concurrent.atomic.AtomicInteger lastCycle) {
        GamaMessage msg = messageParser.parse(raw);
        switch (msg) {
            case GamaMessage.StatusInform si -> {
                if (lastCycle.getAndSet(si.cycle()) != si.cycle()) {
                    publish(runId, "PROGRESS", si.cycle(), si.content(), null);
                }
            }
            case GamaMessage.SimulationOutput so -> {
                publish(runId, "LOG", 0, so.content(), null);
                // Les messages `status` de certaines versions de GAMA ne portent pas le cycle :
                // on l'extrait des lignes « cycle N … » écrites par le modèle pour alimenter la
                // barre de progression. Best-effort, sans incidence si le motif est absent.
                Integer cycle = extractCycle(so.content());
                if (cycle != null && lastCycle.getAndSet(cycle) != cycle) {
                    publish(runId, "PROGRESS", cycle, null, null);
                }
            }
            case GamaMessage.SimulationEnded se -> publish(runId, "ENDED", 0, null, null);
            case GamaMessage.SimulationError se -> publish(runId, "ERROR", 0, se.content(), se.content());
            // Statut d'expérience passé en ERREUR : remonte comme erreur (le run sera marqué ECHEC).
            case GamaMessage.StatusError se -> publish(runId, "ERROR", 0, se.content(), se.content());
            default -> { /* ignoré */ }
        }
    }

    private static final java.util.regex.Pattern CYCLE_PATTERN =
            java.util.regex.Pattern.compile("cycle\\s+(\\d+)");

    private static Integer extractCycle(String content) {
        if (content == null || content.isBlank()) return null;
        var m = CYCLE_PATTERN.matcher(content);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    private void publish(UUID runId, String type, int cycle, String message, String error) {
        rabbitTemplate.convertAndSend(runUpdatesExchange, "",
                new RunUpdateMessage(runId, type, cycle, message, error));
    }
}
