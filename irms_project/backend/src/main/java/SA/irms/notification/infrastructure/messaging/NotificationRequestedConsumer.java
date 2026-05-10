package SA.irms.notification.infrastructure.messaging;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import SA.irms.notification.application.port.in.NotificationRequestConsumerUseCase;
import SA.irms.common.messaging.ManualAckConsumerSupport;

@Component
@ConditionalOnProperty(name = "irms.rabbitmq.enabled", havingValue = "true")
public class NotificationRequestedConsumer {
    public static final String CONSUMER_NAME = "notification.notification-request.delivery";

    private final ManualAckConsumerSupport ackSupport;
    private final NotificationRequestConsumerUseCase materializer;

    public NotificationRequestedConsumer(ManualAckConsumerSupport ackSupport, NotificationRequestConsumerUseCase materializer) {
        this.ackSupport = ackSupport;
        this.materializer = materializer;
    }

    @RabbitListener(queues = "irms.notification.requested.q")
    public void consume(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, CONSUMER_NAME); }

    @RabbitListener(queues = "irms.notification.kitchen-dish-status.q")
    public void kitchenDishNotification(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "notification.kitchen-dish-status.delivery"); }

    @RabbitListener(queues = "irms.notification.payment-completed.q")
    public void paymentNotification(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "notification.payment-completed.delivery"); }

    @RabbitListener(queues = "irms.notification.refund-issued.q")
    public void refundNotification(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "notification.refund-issued.delivery"); }

    @RabbitListener(queues = "irms.notification.receipt-generated.q")
    public void receiptNotification(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "notification.receipt-generated.delivery"); }

    @RabbitListener(queues = "irms.notification.order-cancelled.q")
    public void orderCancelledNotification(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "notification.order-cancelled.delivery"); }

    @RabbitListener(queues = "irms.notification.low-stock.q")
    public void lowStockNotification(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "notification.low-stock.delivery"); }

    @RabbitListener(queues = "irms.notification.reservation-created.q")
    public void reservationCreatedNotification(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "notification.reservation-created.delivery"); }

    @RabbitListener(queues = "irms.notification.reservation-seated.q")
    public void reservationSeatedNotification(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "notification.reservation-seated.delivery"); }

    @RabbitListener(queues = "irms.notification.waitlist-updated.q")
    public void waitlistNotification(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "notification.waitlist-updated.delivery"); }

    @RabbitListener(queues = "irms.notification.menu-availability.q")
    public void menuAvailabilityNotification(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "notification.menu-availability.delivery"); }

    @RabbitListener(queues = "irms.notification.promotion-updated.q")
    public void promotionNotification(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "notification.promotion-updated.delivery"); }

    private void consumeAs(Message message, Channel channel, String consumerName) throws java.io.IOException {
        ackSupport.handle(message, channel, consumerName, materializer::materialize);
    }
}
