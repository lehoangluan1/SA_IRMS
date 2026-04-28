package SA.irms.ordering.application.workflow;

import java.util.Map;

import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.outbox.DomainEventPublisher;

@Component
public class PublishOrderCancellationFollowUpStep {
    private final DomainEventPublisher outboxPublisher;

    public PublishOrderCancellationFollowUpStep(DomainEventPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    public void execute(OrderCancellationContext context) {
        EventEnvelope event = context.event();
        outboxPublisher.publish("NotificationRequested", 1, "Notification", context.orderId(), Map.of(
                "channel", "in_app",
                "type", "order_cancelled",
                "templateCode", "ORDER_CANCELLED",
                "title", "Order cancelled",
                "body", "Order " + context.orderId() + " was cancelled. " + context.reason(),
                "priority", "high"
        ), event.metadata().correlationId(), event.metadata().eventId().toString(), null);
        outboxPublisher.publish("AuditRecordingRequested", 1, "AuditRecord", context.orderId(), Map.of(
                "action", "order.fulfillment.cancelled",
                "aggregateType", "Order",
                "aggregateId", context.orderId(),
                "reason", context.reason()
        ), event.metadata().correlationId(), event.metadata().eventId().toString(), null);
    }
}
