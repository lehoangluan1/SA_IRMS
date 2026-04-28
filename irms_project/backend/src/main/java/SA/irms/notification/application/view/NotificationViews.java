package SA.irms.notification.application.view;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class NotificationViews {
    private NotificationViews() {}

    public record NotificationView(
            UUID id,
            String type,
            String title,
            String body,
            String priority,
            String status,
            Instant createdAt,
            Instant readAt,
            Map<String, Object> payload
    ) {}
}
