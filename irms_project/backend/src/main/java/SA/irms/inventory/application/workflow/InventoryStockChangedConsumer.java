package SA.irms.inventory.application.workflow;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import SA.irms.common.messaging.ManualAckConsumerSupport;

@Component
@ConditionalOnProperty(name = "irms.rabbitmq.enabled", havingValue = "true")
public class InventoryStockChangedConsumer {
    public static final String LOW_STOCK_CONSUMER = "inventory.low-stock.evaluator";
    public static final String REORDER_CONSUMER = "inventory.reorder-suggestion.projector";

    private final ManualAckConsumerSupport ackSupport;
    private final InventoryAlertMediator mediator;

    public InventoryStockChangedConsumer(ManualAckConsumerSupport ackSupport, InventoryAlertMediator mediator) {
        this.ackSupport = ackSupport;
        this.mediator = mediator;
    }

    @RabbitListener(queues = "irms.inventory.low-stock.q")
    public void lowStock(Message message, Channel channel) throws java.io.IOException {
        ackSupport.handle(message, channel, LOW_STOCK_CONSUMER, mediator::handleStockChanged);
    }

    @RabbitListener(queues = "irms.inventory.reorder-suggestion.q")
    public void reorderSuggestion(Message message, Channel channel) throws java.io.IOException {
        ackSupport.handle(message, channel, REORDER_CONSUMER, mediator::handleReorderSuggestion);
    }
}
