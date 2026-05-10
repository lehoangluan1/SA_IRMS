package SA.irms.billing.application.workflow;

import java.util.Map;

import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;
import SA.irms.common.outbox.DomainEventPublisher;

@Component
public class BillingNotificationRequestStep implements BillingSettlementStep {
    private final DomainEventPublisher outboxPublisher;

    BillingNotificationRequestStep(DomainEventPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    @Override
    public String name() {
        return "NOTIFICATION_REQUESTED";
    }

    @Override
    public boolean supports(EventEnvelope event) {
        return ServiceEventTypes.PAYMENT_COMPLETED.equals(event.metadata().eventType())
                || ServiceEventTypes.REFUND_ISSUED.equals(event.metadata().eventType());
    }

    @Override
    public void execute(EventEnvelope event) {
        boolean refund = ServiceEventTypes.REFUND_ISSUED.equals(event.metadata().eventType());
        String type = refund ? "refund_issued" : "payment_completed";
        String title = refund ? "Refund issued" : "Payment completed";
        String body = title + " for " + event.metadata().aggregateId() + ".";
        outboxPublisher.publish("NotificationRequested", 1, "Notification", event.metadata().aggregateId(), Map.of(
                "channel", "in_app",
                "type", type,
                "title", title,
                "body", body,
                "priority", "medium"
        ), event.metadata().correlationId(), event.metadata().eventId().toString(), null);
    }
}
