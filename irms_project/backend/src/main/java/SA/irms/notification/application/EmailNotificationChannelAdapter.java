package SA.irms.notification.application;

import org.springframework.stereotype.Component;

import SA.irms.notification.application.port.out.EmailNotificationChannel;

import SA.irms.common.error.ConflictException;
import SA.irms.common.notification.NotificationCommand;

@Component
public class EmailNotificationChannelAdapter implements EmailNotificationChannel {
    @Override
    public String channel() {
        return "email";
    }

    @Override
    public void validate(NotificationCommand command) {
        if (command.recipientAddress() == null || command.recipientAddress().isBlank()) {
            throw new ConflictException("An email recipient address is required.");
        }
    }
}
