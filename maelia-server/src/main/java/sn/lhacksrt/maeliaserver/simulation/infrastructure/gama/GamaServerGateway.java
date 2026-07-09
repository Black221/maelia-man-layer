package sn.lhacksrt.maeliaserver.simulation.infrastructure.gama;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import sn.lhacksrt.maeliaserver.simulation.domain.model.SimulationRun;
import sn.lhacksrt.maeliaserver.simulation.domain.port.out.GamaGateway;
import sn.lhacksrt.maeliaserver.simulation.domain.port.out.GamaSession;
import sn.lhacksrt.maeliaserver.simulation.infrastructure.gama.message.GamaMessageParser;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Implémente GamaGateway via le protocole WebSocket de GAMA headless.
 * Stratégie : volume partagé (le modèle et les includes sont sur le volume
 * monté par le worker et par le conteneur GAMA — architecture-backend.md §7.3).
 */
@Component
public class GamaServerGateway implements GamaGateway {

    private static final Logger log = LoggerFactory.getLogger(GamaServerGateway.class);

    private final WebSocketClient webSocketClient;
    private final GamaMessageParser messageParser;

    @Value("${gama.ws-url:ws://localhost:6868}")
    private String gamaWsUrl;

    @Value("${gama.command-timeout-seconds:120}")
    private long commandTimeoutSeconds;

    public GamaServerGateway(WebSocketClient gamaWebSocketClient,
                              GamaMessageParser messageParser) {
        this.webSocketClient = gamaWebSocketClient;
        this.messageParser = messageParser;
    }

    @Override
    public GamaSession open(SimulationRun run) throws Exception {
        log.info("Opening GAMA session for run={} url={}", run.getId(), gamaWsUrl);

        GamaWebSocketHandler handler = new GamaWebSocketHandler();
        webSocketClient.execute(handler, new WebSocketHttpHeaders(), URI.create(gamaWsUrl));

        // Attendre que la connexion WebSocket soit établie (max 30s)
        handler.getConnectionFuture().get(30, TimeUnit.SECONDS);
        log.info("GAMA WebSocket connected for run={}", run.getId());

        return new GamaServerSession(handler, messageParser);
    }
}
