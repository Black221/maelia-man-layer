package sn.lhacksrt.maeliaserver.simulation.infrastructure.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Relais (profil API) : consomme les mises à jour de run publiées par le worker sur
 * l'échange fanout (file anonyme liée par {@link RunUpdateRelayConfig}) et les pousse
 * vers le canal STOMP /topic/runs/{runId}.
 *
 * Listener uniquement : les beans Queue/Binding sont déclarés à part pour éviter un
 * cycle de dépendance (une @Configuration portant à la fois des @Bean, un @RabbitListener
 * et une injection se référence elle-même).
 */
@Component
@Profile("api")
public class RunUpdateRelay {

    private final SimpMessagingTemplate stomp;

    public RunUpdateRelay(SimpMessagingTemplate stomp) {
        this.stomp = stomp;
    }

    @RabbitListener(queues = "#{runUpdatesQueue.name}")
    public void relay(RunUpdateMessage msg) {
        stomp.convertAndSend("/topic/runs/" + msg.runId(), msg);
    }
}
