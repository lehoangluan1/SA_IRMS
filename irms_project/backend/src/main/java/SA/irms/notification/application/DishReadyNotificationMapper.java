package SA.irms.notification.application;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;
import SA.irms.common.notification.NotificationCommand;

@Component
public class DishReadyNotificationMapper implements NotificationEventMapper {
    @Override
    public boolean supports(EventEnvelope envelope) {
        return ServiceEventTypes.KITCHEN_DISH_STATUS_CHANGED.equals(envelope.metadata().eventType())
                && isReady(envelope.payload().get("status"));
    }

    @Override
    public Optional<NotificationCommand> map(EventEnvelope envelope) {
        Map<String, Object> payload = envelope.payload();
        return Optional.of(new NotificationCommand(
                null,
                null,
                null,
                null,
                string(payload, "channel", "in_app"),
                "kitchen_ready",
                "DISH_READY",
                payload,
                "Dish ready",
                "Dish " + string(payload, "orderItemId", envelope.metadata().aggregateId()) + " is ready to serve.",
                string(payload, "recipientRole", "server"),
                uuid(payload, "serverId"),
                string(payload, "recipientAddress", "server"),
                "high"
        ));
    }

    private boolean isReady(Object value) {
        String status = value == null ? "" : value.toString().trim().toUpperCase();
        return status.equals("READY") || status.equals("READY_TO_SERVE");
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
