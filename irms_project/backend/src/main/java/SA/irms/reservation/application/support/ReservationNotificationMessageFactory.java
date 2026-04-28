package SA.irms.reservation.application.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.common.error.ConflictException;
import SA.irms.reservation.application.query.ReservationNotificationTarget;
import SA.irms.common.notification.NotificationCommand;

@Component
public class ReservationNotificationMessageFactory {
    public NotificationCommand customerNotification(
            UUID reservationId,
            UUID waitlistEntryId,
            ReservationNotificationTarget target,
            String title,
            String body,
            String channel
    ) {
        String normalizedChannel = normalizeChannel(channel);
        String recipientAddress = resolveRecipientAddress(target, normalizedChannel);
        Map<String, Object> payload = new LinkedHashMap<>();
        if (reservationId != null) {
            payload.put("reservationId", reservationId.toString());
        }
        if (waitlistEntryId != null) {
            payload.put("waitlistEntryId", waitlistEntryId.toString());
        }
        return new NotificationCommand(
                null,
                reservationId,
                waitlistEntryId,
                null,
                normalizedChannel,
                reservationId != null ? "reservation_update" : "waitlist_turn_available",
                reservationId != null ? "RESERVATION_NOTIFY" : "WAITLIST_NOTIFY",
                payload,
                title,
                body,
                null,
                null,
                recipientAddress,
                "in_app".equals(normalizedChannel) ? "medium" : "high"
        );
    }

    public List<NotificationCommand> staffNotifications(
            UUID reservationId,
            String type,
            String templateCode,
            String title,
            String body
    ) {
        return List.of("host", "manager").stream()
                .map(role -> new NotificationCommand(
                        null,
                        reservationId,
                        null,
                        null,
                        "in_app",
                        type,
                        templateCode,
                        Map.of("reservationId", reservationId.toString()),
                        title,
                        body,
                        role,
                        null,
                        null,
                        "high"
                ))
                .toList();
    }

    private String normalizeChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            throw new ConflictException("A notification channel is required.");
        }
        String normalized = channel.trim().toLowerCase();
        if (!List.of("sms", "email", "in_app").contains(normalized)) {
            throw new ConflictException("Unsupported notification channel.");
        }
        return normalized;
    }

    private String resolveRecipientAddress(ReservationNotificationTarget target, String channel) {
        return switch (channel) {
            case "sms" -> {
                if (target.phone() == null || target.phone().isBlank()) {
                    throw new ConflictException("The selected contact does not have a valid phone number for SMS delivery.");
                }
                yield target.phone();
            }
            case "email" -> {
                if (target.email() == null || target.email().isBlank()) {
                    throw new ConflictException("The selected contact does not have a valid email address for delivery.");
                }
                yield target.email();
            }
            case "in_app" -> target.name();
            default -> throw new ConflictException("Unsupported notification channel.");
        };
    }
}
