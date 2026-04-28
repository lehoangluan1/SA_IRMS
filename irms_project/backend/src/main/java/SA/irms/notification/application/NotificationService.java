package SA.irms.notification.application;

import java.util.Map;
import java.util.UUID;

public interface NotificationService {
    QueuedNotification queue(Command command);

    record Command(
            UUID lowStockAlertId,
            UUID reservationId,
            UUID waitlistEntryId,
            UUID paymentId,
            String channel,
            String type,
            String templateCode,
            Map<String, Object> payload,
            String title,
            String body,
            String recipientRole,
            UUID recipientUserId,
            String recipientAddress,
            String priority
    ) {
    }

    record QueuedNotification(UUID messageId, String channel, String status) {
    }
}
