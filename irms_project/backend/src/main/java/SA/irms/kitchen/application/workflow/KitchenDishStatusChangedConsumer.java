package SA.irms.kitchen.application.workflow;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import SA.irms.common.messaging.ManualAckConsumerSupport;

@Component
@ConditionalOnProperty(name = "irms.rabbitmq.enabled", havingValue = "true")
public class KitchenDishStatusChangedConsumer {
    public static final String CONSUMER_NAME = "kitchen.dish-status.workflow";

    private final ManualAckConsumerSupport ackSupport;
    private final KitchenWorkflowMediator mediator;

    public KitchenDishStatusChangedConsumer(ManualAckConsumerSupport ackSupport, KitchenWorkflowMediator mediator) {
        this.ackSupport = ackSupport;
        this.mediator = mediator;
    }

    @RabbitListener(queues = "irms.kitchen.dish-status.workflow.q")
    public void consume(Message message, Channel channel) throws java.io.IOException {
        ackSupport.handle(message, channel, CONSUMER_NAME, mediator::handleDishStatusChanged);
    }
}
