package SA.irms.kitchen.application.workflow;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.outbox.DomainEventPublisher;

@Component
public class KitchenReadyNotificationStep {
    private static final Set<String> READY_STATES = Set.of("READY", "READY_TO_SERVE");

    private final DomainEventPublisher outboxPublisher;

    public KitchenReadyNotificationStep(DomainEventPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    public void execute(EventEnvelope envelope) {
        String status = String.valueOf(envelope.payload().getOrDefault("status", envelope.payload().getOrDefault("type", ""))).toUpperCase();
        if (!READY_STATES.contains(status)) {
            return;
        }
        outboxPublisher.publish("NotificationRequested", 1, "Notification", envelope.metadata().aggregateId(), Map.of(
                "channel", "in_app",
                "type", "kitchen_ready",
                "title", "Dish ready",
                "body", "A dish is ready to serve.",
                "priority", "high"
        ), envelope.metadata().correlationId(), envelope.metadata().eventId().toString(), null);
    }
}
