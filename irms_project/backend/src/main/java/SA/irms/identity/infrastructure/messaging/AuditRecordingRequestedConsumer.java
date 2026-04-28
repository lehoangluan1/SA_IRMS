package SA.irms.identity.infrastructure.messaging;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import SA.irms.identity.application.port.in.AuditRecordingConsumerUseCase;
import SA.irms.common.messaging.ManualAckConsumerSupport;

@Component
@ConditionalOnProperty(name = "irms.rabbitmq.enabled", havingValue = "true")
public class AuditRecordingRequestedConsumer {
    public static final String CONSUMER_NAME = "identity.audit-recording.materializer";

    private final ManualAckConsumerSupport ackSupport;
    private final AuditRecordingConsumerUseCase materializer;

    public AuditRecordingRequestedConsumer(ManualAckConsumerSupport ackSupport, AuditRecordingConsumerUseCase materializer) {
        this.ackSupport = ackSupport;
        this.materializer = materializer;
    }

    @RabbitListener(queues = "irms.audit.recording-requested.q")
    public void auditRequested(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, CONSUMER_NAME); }

    @RabbitListener(queues = "irms.audit.order-confirmed.q")
    public void orderConfirmedAudit(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "identity.audit.order-confirmed"); }

    @RabbitListener(queues = "irms.audit.order-cancelled.q")
    public void orderCancelledAudit(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "identity.audit.order-cancelled"); }

    @RabbitListener(queues = "irms.audit.kitchen-ticket-created.q")
    public void kitchenTicketCreatedAudit(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "identity.audit.kitchen-ticket-created"); }

    @RabbitListener(queues = "irms.audit.kitchen-dish-status.q")
    public void kitchenAudit(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "identity.audit.kitchen-dish-status"); }

    @RabbitListener(queues = "irms.audit.inventory-stock.q")
    public void inventoryAudit(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "identity.audit.inventory-stock"); }

    @RabbitListener(queues = "irms.audit.low-stock.q")
    public void lowStockAudit(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "identity.audit.low-stock"); }

    @RabbitListener(queues = "irms.audit.bill-issued.q")
    public void billIssuedAudit(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "identity.audit.bill-issued"); }

    @RabbitListener(queues = "irms.audit.payment-completed.q")
    public void paymentAudit(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "identity.audit.payment-completed"); }

    @RabbitListener(queues = "irms.audit.refund-issued.q")
    public void refundAudit(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "identity.audit.refund-issued"); }

    @RabbitListener(queues = "irms.audit.receipt-generated.q")
    public void receiptAudit(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "identity.audit.receipt-generated"); }

    @RabbitListener(queues = "irms.audit.reservation-created.q")
    public void reservationCreatedAudit(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "identity.audit.reservation-created"); }

    @RabbitListener(queues = "irms.audit.reservation-seated.q")
    public void reservationSeatedAudit(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "identity.audit.reservation-seated"); }

    @RabbitListener(queues = "irms.audit.waitlist-updated.q")
    public void waitlistAudit(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "identity.audit.waitlist-updated"); }

    @RabbitListener(queues = "irms.audit.menu-availability.q")
    public void menuAvailabilityAudit(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "identity.audit.menu-availability"); }

    @RabbitListener(queues = "irms.audit.promotion-updated.q")
    public void promotionAudit(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "identity.audit.promotion-updated"); }

    @RabbitListener(queues = "irms.audit.staff-shift.q")
    public void staffShiftAudit(Message message, Channel channel) throws java.io.IOException { consumeAs(message, channel, "identity.audit.staff-shift"); }

    private void consumeAs(Message message, Channel channel, String consumerName) throws java.io.IOException {
        ackSupport.handle(message, channel, consumerName, materializer::record);
    }
}
