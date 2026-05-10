package SA.irms.notification.application;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;
import SA.irms.common.notification.NotificationCommand;

@Component
public class OrderCancelledNotificationMapper implements NotificationEventMapper {
    @Override
    public boolean supports(EventEnvelope envelope) {
        return ServiceEventTypes.ORDER_CANCELLED.equals(envelope.metadata().eventType());
    }

    @Override
    public Optional<NotificationCommand> map(EventEnvelope envelope) {
        Map<String, Object> payload = envelope.payload();
        return Optional.of(new NotificationCommand(
                null,
                null,
                null,
                uuid(payload, "paymentId"),
                string(payload, "channel", "in_app"),
                "order_cancelled",
                "ORDER_CANCELLED",
                payload,
                "Order cancelled",
                "Order " + envelope.metadata().aggregateId() + " was cancelled. " + string(payload, "reason", ""),
                string(payload, "recipientRole", "manager"),
                uuid(payload, "recipientUserId"),
                string(payload, "recipientAddress", "manager"),
                "high"
        ));
    }

    private UUID uuid(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null || value.toString().isBlank() ? null : UUID.fromString(value.toString());
    }

    private String string(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }
}
