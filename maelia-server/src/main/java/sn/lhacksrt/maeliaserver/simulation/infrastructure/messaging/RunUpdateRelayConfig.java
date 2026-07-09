package sn.lhacksrt.maeliaserver.simulation.infrastructure.messaging;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Infrastructure AMQP du relais de mises à jour (profil API) : chaque instance d'API lie
 * sa propre file anonyme (auto-delete) à l'échange fanout des mises à jour, de sorte que
 * toutes reçoivent la progression et la relaient à leurs clients STOMP ({@link RunUpdateRelay}).
 *
 * Séparé du listener pour éviter un cycle de dépendance de bean.
 */
@Configuration
@Profile("api")
public class RunUpdateRelayConfig {

    @Bean
    public Queue runUpdatesQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding runUpdatesBinding(Queue runUpdatesQueue, FanoutExchange runUpdatesExchange) {
        return BindingBuilder.bind(runUpdatesQueue).to(runUpdatesExchange);
    }
}
