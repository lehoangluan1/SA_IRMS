package SA.irms.billing.application.workflow;

import java.util.Map;

import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;
import SA.irms.common.outbox.DomainEventPublisher;

@Component
public class BillingAuditRequestStep implements BillingSettlementStep {
    private final DomainEventPublisher outboxPublisher;

    BillingAuditRequestStep(DomainEventPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    @Override
    public String name() {
        return "AUDIT_REQUESTED";
    }

    @Override
    public boolean supports(EventEnvelope event) {
        return ServiceEventTypes.PAYMENT_COMPLETED.equals(event.metadata().eventType())
                || ServiceEventTypes.REFUND_ISSUED.equals(event.metadata().eventType());
    }

    @Override
    public void execute(EventEnvelope event) {
        String aggregateId = event.metadata().aggregateId();
        String action = ServiceEventTypes.REFUND_ISSUED.equals(event.metadata().eventType()) ? "refund.issued" : "payment.completed";
        outboxPublisher.publish("AuditRecordingRequested", 1, "AuditRecord", aggregateId, Map.of(
                "action", action,
                "aggregateId", aggregateId,
                "eventType", event.metadata().eventType(),
                "amount", event.payload().getOrDefault("amount", "0"),
                "reason", event.payload().getOrDefault("reason", action)
        ), event.metadata().correlationId(), event.metadata().eventId().toString(), null);
    }
}
