package SA.irms.notification.application;

import org.springframework.stereotype.Component;

import SA.irms.notification.application.port.out.InAppNotificationChannel;

import SA.irms.common.error.ConflictException;
import SA.irms.common.notification.NotificationCommand;

@Component
public class InAppNotificationChannelAdapter implements InAppNotificationChannel {
    @Override
    public String channel() {
        return "in_app";
    }

    @Override
    public void validate(NotificationCommand command) {
        if (command.recipientRole() == null
                && command.recipientUserId() == null
                && (command.recipientAddress() == null || command.recipientAddress().isBlank())) {
            throw new ConflictException("An in-app notification requires a role, user, or in-app target.");
        }
    }
}
