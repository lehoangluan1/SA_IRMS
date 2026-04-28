package SA.irms.billing.application.workflow;

import java.util.Map;

import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;
import SA.irms.billing.application.events.ReceiptGeneratedEvent;
import SA.irms.common.outbox.DomainEventPublisher;

@Component
public class BillingReceiptRequestStep implements BillingSettlementStep {
    private final DomainEventPublisher outboxPublisher;

    BillingReceiptRequestStep(DomainEventPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    @Override
    public String name() {
        return "RECEIPT_REQUESTED";
    }

    @Override
    public boolean supports(EventEnvelope event) {
        return ServiceEventTypes.PAYMENT_COMPLETED.equals(event.metadata().eventType());
    }

    @Override
    public void execute(EventEnvelope event) {
        String paymentId = event.metadata().aggregateId();
        String billId = String.valueOf(event.payload().getOrDefault("billId", "unknown"));
        outboxPublisher.publish(new ReceiptGeneratedEvent(paymentId, Map.of(
                "paymentId", paymentId,
                "billId", billId,
                "amount", event.payload().getOrDefault("amount", "0")
        )), event.metadata().correlationId(), event.metadata().eventId().toString());
    }
}
