package SA.irms.common.messaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import SA.irms.common.events.EventRouting;
import SA.irms.common.events.EventRoutingRegistry;

@Configuration
@ConditionalOnProperty(name = "irms.rabbitmq.enabled", havingValue = "true")
public class RabbitMqTopologyConfig {
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter jsonMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setPrefetchCount(10);
        return factory;
    }

    @Bean
    public Declarables irmsRabbitTopology() {
        List<Declarable> declarables = new ArrayList<>();
        Map<String, TopicExchange> exchanges = new HashMap<>();
        for (String exchangeName : List.of(
                EventRoutingRegistry.ORDER_EXCHANGE,
                EventRoutingRegistry.KITCHEN_EXCHANGE,
                EventRoutingRegistry.INVENTORY_EXCHANGE,
                EventRoutingRegistry.BILLING_EXCHANGE,
                EventRoutingRegistry.RESERVATION_EXCHANGE,
                EventRoutingRegistry.NOTIFICATION_EXCHANGE,
                EventRoutingRegistry.AUDIT_EXCHANGE,
                EventRoutingRegistry.REPORTING_EXCHANGE
        )) {
            TopicExchange exchange = new TopicExchange(exchangeName, true, false);
            exchanges.put(exchangeName, exchange);
            declarables.add(exchange);
        }
        DirectExchange dlx = new DirectExchange(EventRoutingRegistry.DLX_EXCHANGE, true, false);
        DirectExchange retry = new DirectExchange(EventRoutingRegistry.RETRY_EXCHANGE, true, false);
        declarables.add(dlx);
        declarables.add(retry);

        EventRoutingRegistry.routing().forEach((eventType, routing) -> {
            TopicExchange exchange = exchanges.get(routing.exchangeName());
            for (String queueName : EventRoutingRegistry.queuesFor(eventType)) {
                Queue queue = durableQueue(queueName);
                Queue retryQueue = retryQueue(queueName + ".retry", queueName);
                Queue deadLetterQueue = new Queue(queueName + ".dlq", true);
                declarables.add(queue);
                declarables.add(retryQueue);
                declarables.add(deadLetterQueue);
                declarables.add(BindingBuilder.bind(queue).to(exchange).with(routing.routingKey()));
                declarables.add(BindingBuilder.bind(retryQueue).to(retry).with(queueName + ".retry"));
                declarables.add(BindingBuilder.bind(deadLetterQueue).to(dlx).with(queueName + ".dlq"));
            }
        });
        return new Declarables(declarables);
    }

    private Queue durableQueue(String name) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EventRoutingRegistry.RETRY_EXCHANGE);
        args.put("x-dead-letter-routing-key", name + ".retry");
        return new Queue(name, true, false, false, args);
    }

    private Queue retryQueue(String name, String originalQueueName) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "");
        args.put("x-dead-letter-routing-key", originalQueueName);
        return new Queue(name, true, false, false, args);
    }
}
