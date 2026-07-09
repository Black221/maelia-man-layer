package sn.lhacksrt.maeliaserver.bootstrap.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Value("${maelia.messaging.run-exchange}")
    private String runExchange;

    @Value("${maelia.messaging.run-queue}")
    private String runQueue;

    @Value("${maelia.messaging.run-routing-key}")
    private String runRoutingKey;

    @Value("${maelia.messaging.run-updates-exchange:maelia.run.updates}")
    private String runUpdatesExchange;

    @Bean
    public DirectExchange runExchange() {
        return new DirectExchange(runExchange, true, false);
    }

    /**
     * Échange fanout des mises à jour de run : le worker y publie la progression,
     * chaque instance d'API y lie une file anonyme et relaie vers STOMP (cf. RunUpdateRelay).
     */
    @Bean
    public FanoutExchange runUpdatesExchange() {
        return new FanoutExchange(runUpdatesExchange, true, false);
    }

    @Bean
    public Queue runQueue() {
        return QueueBuilder.durable(runQueue).build();
    }

    @Bean
    public Binding runBinding(Queue runQueue, DirectExchange runExchange) {
        return BindingBuilder.bind(runQueue).to(runExchange).with(runRoutingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);
        return template;
    }
}
