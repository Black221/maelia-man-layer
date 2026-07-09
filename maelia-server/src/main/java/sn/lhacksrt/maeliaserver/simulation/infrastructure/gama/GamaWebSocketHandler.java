package sn.lhacksrt.maeliaserver.simulation.infrastructure.gama;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Adaptateur WebSocket bas niveau vers le serveur headless GAMA.
 * Protocole GAMA (détails dans architecture-backend.md §7).
 * TODO M1 : parsing complet des GamaMessage (sealed interface) + routage par run_id.
 */
public class GamaWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GamaWebSocketHandler.class);

    @Getter
    private volatile WebSocketSession session;
    @Getter
    private final CompletableFuture<Void> connectionFuture = new CompletableFuture<>();
    @Setter
    private Consumer<String> messageListener;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.session = session;
        log.info("GAMA WebSocket connected, session={}", session.getId());
        connectionFuture.complete(null);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        // TRACE : un log par message GAMA (statuts en flot continu pendant un run) — trop verbeux en DEBUG.
        log.trace("GAMA message: {}", payload);
        if (messageListener != null) {
            messageListener.accept(payload);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("GAMA transport error: {}", exception.getMessage(), exception);
        connectionFuture.completeExceptionally(exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("GAMA connection closed, code={} reason={}", Optional.of(status.getCode()), status.getReason());
        this.session = null;
    }

    public void sendCommand(String jsonCommand) throws Exception {
        if (session == null || !session.isOpen()) {
            throw new IllegalStateException("GAMA WebSocket session is not open");
        }
        log.debug("GAMA command: {}", jsonCommand);
        session.sendMessage(new TextMessage(jsonCommand));
    }

    public boolean isConnected() { return session != null && session.isOpen(); }
}
