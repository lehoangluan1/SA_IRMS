package SA.irms.inventory.application.workflow;

import java.util.Map;

import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.outbox.DomainEventPublisher;

@Component
public class RequestManagerNotificationStep {
    private final DomainEventPublisher outboxPublisher;

    public RequestManagerNotificationStep(DomainEventPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    public void execute(EventEnvelope event, EvaluateLowStockStep.Evaluation evaluation) {
        outboxPublisher.publish("NotificationRequested", 1, "Notification", evaluation.inventoryItemId().toString(), Map.of(
                "channel", "in_app",
                "type", "low_stock",
                "title", "Low stock detected",
                "body", "Inventory item " + evaluation.inventoryItemId() + " is below threshold.",
                "priority", "high"
        ), event.metadata().correlationId(), event.metadata().eventId().toString(), null);
    }
}
