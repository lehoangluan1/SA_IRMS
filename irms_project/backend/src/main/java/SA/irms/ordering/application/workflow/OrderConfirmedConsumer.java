package SA.irms.ordering.application.workflow;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import SA.irms.common.messaging.ManualAckConsumerSupport;

@Component
@ConditionalOnProperty(name = "irms.rabbitmq.enabled", havingValue = "true")
public class OrderConfirmedConsumer {
    public static final String ORDER_CONFIRMED_CONSUMER = "ordering.order-fulfillment.mediator";
    public static final String DISH_STATUS_CONSUMER = "ordering.order-fulfillment.dish-status";
    public static final String ORDER_CANCELLED_CONSUMER = "ordering.order-fulfillment.order-cancelled";

    private final ManualAckConsumerSupport ackSupport;
    private final OrderFulfillmentMediator mediator;

    public OrderConfirmedConsumer(ManualAckConsumerSupport ackSupport, OrderFulfillmentMediator mediator) {
        this.ackSupport = ackSupport;
        this.mediator = mediator;
    }

    @RabbitListener(queues = "irms.order-fulfillment.order-confirmed.q")
    public void consumeOrderConfirmed(Message message, Channel channel) throws java.io.IOException {
        ackSupport.handle(message, channel, ORDER_CONFIRMED_CONSUMER, mediator::handleOrderConfirmed);
    }

    @RabbitListener(queues = "irms.order-fulfillment.kitchen-dish-status.q")
    public void consumeKitchenDishStatus(Message message, Channel channel) throws java.io.IOException {
        ackSupport.handle(message, channel, DISH_STATUS_CONSUMER, mediator::handleDishStatusChanged);
    }

    @RabbitListener(queues = "irms.order-fulfillment.order-cancelled.q")
    public void consumeOrderCancelled(Message message, Channel channel) throws java.io.IOException {
        ackSupport.handle(message, channel, ORDER_CANCELLED_CONSUMER, mediator::handleOrderCancelled);
    }
}
