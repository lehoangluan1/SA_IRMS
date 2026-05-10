package SA.irms.billing.application.workflow;

import java.util.Map;

import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;
import SA.irms.common.outbox.DomainEventPublisher;

@Component
public class BillingReportingProjectionStep implements BillingSettlementStep {
    private final DomainEventPublisher outboxPublisher;

    BillingReportingProjectionStep(DomainEventPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    @Override
    public String name() {
        return "REPORTING_PROJECTED";
    }

    @Override
    public boolean supports(EventEnvelope event) {
        return ServiceEventTypes.PAYMENT_COMPLETED.equals(event.metadata().eventType());
    }

    @Override
    public void execute(EventEnvelope event) {
        String paymentId = event.metadata().aggregateId();
        outboxPublisher.publish(ServiceEventTypes.BILLING_SETTLEMENT_PROJECTED, 1, "Payment", paymentId, Map.of(
                "paymentId", paymentId,
                "billId", event.payload().getOrDefault("billId", "unknown"),
                "projection", "payment_completed",
                "amount", event.payload().getOrDefault("amount", "0")
        ), event.metadata().correlationId(), event.metadata().eventId().toString(), "billing-service");
    }
}
