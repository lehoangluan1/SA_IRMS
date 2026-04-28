package SA.irms.reporting.infrastructure.messaging;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import SA.irms.reporting.application.port.out.ReportingProjectionRecorderPort;
import SA.irms.common.messaging.ManualAckConsumerSupport;

@Component
@ConditionalOnProperty(name = "irms.rabbitmq.enabled", havingValue = "true")
public class ReportingProjectionConsumer {
    public static final String KITCHEN_CONSUMER = "reporting.kitchen.projection";
    public static final String INVENTORY_CONSUMER = "reporting.inventory.projection";
    public static final String BILLING_CONSUMER = "reporting.billing.projection";
    public static final String ORDER_CONSUMER = "reporting.order.projection";
    public static final String RESERVATION_CONSUMER = "reporting.reservation.projection";
    public static final String MENU_CONSUMER = "reporting.menu.projection";
    public static final String STAFF_CONSUMER = "reporting.staff.projection";

    private final ManualAckConsumerSupport ackSupport;
    private final ReportingProjectionRecorderPort projectionService;

    public ReportingProjectionConsumer(ManualAckConsumerSupport ackSupport, ReportingProjectionRecorderPort projectionService) {
        this.ackSupport = ackSupport;
        this.projectionService = projectionService;
    }

    @RabbitListener(queues = "irms.reporting.order-confirmed.q")
    public void orderConfirmed(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, ORDER_CONSUMER); }

    @RabbitListener(queues = "irms.reporting.order-cancelled.q")
    public void orderCancelled(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, ORDER_CONSUMER + ".cancelled"); }

    @RabbitListener(queues = "irms.reporting.kitchen-ticket-created.q")
    public void kitchenTicketCreated(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, KITCHEN_CONSUMER + ".ticket-created"); }

    @RabbitListener(queues = "irms.reporting.kitchen-dish-status.q")
    public void kitchenDishStatusChanged(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, KITCHEN_CONSUMER); }

    @RabbitListener(queues = "irms.reporting.inventory-stock.q")
    public void inventoryStockChanged(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, INVENTORY_CONSUMER); }

    @RabbitListener(queues = "irms.reporting.low-stock.q")
    public void lowStock(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, INVENTORY_CONSUMER + ".low-stock"); }

    @RabbitListener(queues = "irms.reporting.bill-issued.q")
    public void billIssued(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, BILLING_CONSUMER + ".bill-issued"); }

    @RabbitListener(queues = "irms.reporting.payment-completed.q")
    public void paymentCompleted(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, BILLING_CONSUMER); }

    @RabbitListener(queues = "irms.reporting.refund-issued.q")
    public void refundIssued(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, BILLING_CONSUMER + ".refund-issued"); }

    @RabbitListener(queues = "irms.reporting.receipt-generated.q")
    public void receiptGenerated(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, BILLING_CONSUMER + ".receipt-generated"); }

    @RabbitListener(queues = "irms.reporting.billing-settlement.q")
    public void billingSettlementProjected(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, BILLING_CONSUMER + ".settlement"); }

    @RabbitListener(queues = "irms.reporting.reservation-created.q")
    public void reservationCreated(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, RESERVATION_CONSUMER + ".created"); }

    @RabbitListener(queues = "irms.reporting.reservation-seated.q")
    public void reservationSeated(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, RESERVATION_CONSUMER + ".seated"); }

    @RabbitListener(queues = "irms.reporting.waitlist-updated.q")
    public void waitlistUpdated(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, RESERVATION_CONSUMER + ".waitlist"); }

    @RabbitListener(queues = "irms.reporting.menu-availability.q")
    public void menuAvailability(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, MENU_CONSUMER + ".availability"); }

    @RabbitListener(queues = "irms.reporting.promotion-updated.q")
    public void promotionUpdated(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, MENU_CONSUMER + ".promotion"); }

    @RabbitListener(queues = "irms.reporting.staff-shift.q")
    public void staffShift(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, STAFF_CONSUMER); }

    private void consumeAs(Message message, Channel channel, String consumerName) throws java.io.IOException {
        ackSupport.handle(message, channel, consumerName, projectionService::recordEventProjection);
    }
}
