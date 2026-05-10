package SA.irms.ordering.application.workflow;

import java.util.Map;

import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.outbox.DomainEventPublisher;

@Component
public class OrderReadyNotificationStep {
    private final DomainEventPublisher outboxPublisher;

    public OrderReadyNotificationStep(DomainEventPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    public void execute(String orderId, EventEnvelope event) {
        outboxPublisher.publish("NotificationRequested", 1, "Notification", orderId, Map.of(
                "channel", "in_app",
                "type", "order_ready",
                "templateCode", "ORDER_READY",
                "title", "Order ready",
                "body", "All dishes for order " + orderId + " are ready.",
                "priority", "high"
        ), event.metadata().correlationId(), event.metadata().eventId().toString(), null);
    }
}
