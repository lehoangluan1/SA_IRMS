package SA.irms.notification.application;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;
import SA.irms.common.notification.NotificationCommand;

@Component
public class PaymentCompletedNotificationMapper implements NotificationEventMapper {
    @Override
    public boolean supports(EventEnvelope envelope) {
        return ServiceEventTypes.PAYMENT_COMPLETED.equals(envelope.metadata().eventType())
                || ServiceEventTypes.RECEIPT_GENERATED.equals(envelope.metadata().eventType());
    }

    @Override
    public Optional<NotificationCommand> map(EventEnvelope envelope) {
        Map<String, Object> payload = envelope.payload();
        return Optional.of(new NotificationCommand(
                null,
                null,
                null,
                uuid(payload, "paymentId", envelope.metadata().aggregateId()),
                string(payload, "channel", "in_app"),
                "payment_completed",
                "PAYMENT_COMPLETED",
                payload,
                "Payment completed",
                "Payment " + envelope.metadata().aggregateId() + " was completed.",
                string(payload, "recipientRole", "cashier"),
                uuid(payload, "recipientUserId", null),
                string(payload, "recipientAddress", "cashier"),
                "medium"
        ));
    }

    private UUID uuid(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        String raw = value == null || value.toString().isBlank() ? fallback : value.toString();
        return raw == null || raw.isBlank() ? null : UUID.fromString(raw);
    }

    private String string(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }
}
