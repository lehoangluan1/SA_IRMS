package SA.irms.notification.application.port.out;

import java.util.UUID;

import SA.irms.common.notification.NotificationCommand;

public interface NotificationQueuePort {
    QueuedNotification queue(NotificationCommand command);

    record QueuedNotification(UUID messageId, String channel, String status) {
    }
}
