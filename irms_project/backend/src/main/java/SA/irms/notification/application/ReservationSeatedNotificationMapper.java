package SA.irms.notification.application;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;
import SA.irms.common.notification.NotificationCommand;

@Component
public class ReservationSeatedNotificationMapper implements NotificationEventMapper {
    @Override
    public boolean supports(EventEnvelope envelope) {
        return ServiceEventTypes.RESERVATION_SEATED.equals(envelope.metadata().eventType())
                || ServiceEventTypes.RESERVATION_CREATED.equals(envelope.metadata().eventType())
                || ServiceEventTypes.WAITLIST_UPDATED.equals(envelope.metadata().eventType());
    }

    @Override
    public Optional<NotificationCommand> map(EventEnvelope envelope) {
        Map<String, Object> payload = envelope.payload();
        return Optional.of(new NotificationCommand(
                null,
                uuid(payload, "reservationId", envelope.metadata().aggregateId()),
                uuid(payload, "waitlistEntryId", null),
                null,
                string(payload, "channel", "sms"),
                "reservation_update",
                "RESERVATION_UPDATE",
                payload,
                "Reservation update",
                "Your reservation is updated for table " + string(payload, "tableNumber", "assigned"),
                null,
                uuid(payload, "recipientUserId", null),
                string(payload, "recipientAddress", "guest"),
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
