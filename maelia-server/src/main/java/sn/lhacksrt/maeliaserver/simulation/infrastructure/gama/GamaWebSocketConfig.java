package sn.lhacksrt.maeliaserver.simulation.infrastructure.gama;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Configuration
public class GamaWebSocketConfig {

    private static final Logger log = LoggerFactory.getLogger(GamaWebSocketConfig.class);

    @Value("${gama.ws-url:ws://localhost:6868}")
    private String gamaWsUrl;

    /**
     * Taille max d'un message WebSocket entrant. Le défaut JSR-356 (8192 o) est trop petit :
     * GAMA envoie parfois de longs messages (statuts/console, gros JSON) — au-delà, le conteneur
     * ferme la connexion avec le code 1009 (« message too big for the output buffer »), ce qui
     * fait échouer le run. On monte à 32 Mo par défaut (configurable).
     */
    @Value("${gama.ws-max-message-bytes:33554432}")
    private int maxMessageBytes;

    @Bean
    public WebSocketClient gamaWebSocketClient() {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxTextMessageBufferSize(maxMessageBytes);
        container.setDefaultMaxBinaryMessageBufferSize(maxMessageBytes);
        // Pas de coupure sur inactivité : un run peut rester silencieux longtemps entre deux statuts.
        container.setDefaultMaxSessionIdleTimeout(0);
        log.info("GAMA WebSocket client: maxTextMessageBufferSize={} octets, idleTimeout désactivé", maxMessageBytes);
        return new StandardWebSocketClient(container);
    }

    public String getGamaWsUrl() { return gamaWsUrl; }
}
