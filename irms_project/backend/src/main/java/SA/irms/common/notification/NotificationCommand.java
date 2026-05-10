package SA.irms.common.notification;

import java.util.Map;
import java.util.UUID;

public record NotificationCommand(
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
