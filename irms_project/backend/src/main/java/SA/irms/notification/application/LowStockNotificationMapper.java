package SA.irms.notification.application;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;
import SA.irms.common.notification.NotificationCommand;

@Component
public class LowStockNotificationMapper implements NotificationEventMapper {
    @Override
    public boolean supports(EventEnvelope envelope) {
        return ServiceEventTypes.LOW_STOCK_DETECTED.equals(envelope.metadata().eventType())
                || ServiceEventTypes.INVENTORY_STOCK_CHANGED.equals(envelope.metadata().eventType());
    }

    @Override
    public Optional<NotificationCommand> map(EventEnvelope envelope) {
        Map<String, Object> payload = envelope.payload();
        return Optional.of(new NotificationCommand(
                uuid(payload, "lowStockAlertId"),
                null,
                null,
                null,
                string(payload, "channel", "in_app"),
                "low_stock",
                "LOW_STOCK_ALERT",
                payload,
                "Low stock detected",
                "Inventory item " + envelope.metadata().aggregateId() + " is below threshold.",
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
