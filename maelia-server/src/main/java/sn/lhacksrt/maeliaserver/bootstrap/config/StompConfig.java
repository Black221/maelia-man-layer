package sn.lhacksrt.maeliaserver.bootstrap.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Canal STOMP exposé au frontend pour la progression des runs (/topic/runs/{id}).
 * Profil API uniquement : le worker (web-application-type none) n'héberge pas de broker —
 * il publie ses mises à jour sur RabbitMQ, relayées ici (cf. RunUpdateRelay).
 */
@Configuration
@Profile("api")
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket natif : le front (@stomp/stompjs avec brokerURL ws://.../ws)
        // ouvre une vraie WebSocket. Indispensable — sans cet enregistrement, seul SockJS
        // répondait et la connexion STOMP native échouait (front sans mises à jour).
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        // Repli SockJS (clients sans WebSocket natif / proxys récalcitrants).
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
