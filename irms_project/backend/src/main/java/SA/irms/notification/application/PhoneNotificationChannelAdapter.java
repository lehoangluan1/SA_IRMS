package SA.irms.notification.application;

import org.springframework.stereotype.Component;

import SA.irms.notification.application.port.out.NotificationChannelAdapter;

import SA.irms.common.error.ConflictException;
import SA.irms.common.notification.NotificationCommand;

@Component
public class PhoneNotificationChannelAdapter implements NotificationChannelAdapter {
    @Override
    public String channel() {
        return "phone";
    }

    @Override
    public void validate(NotificationCommand command) {
        if (command.recipientAddress() == null || command.recipientAddress().isBlank()) {
            throw new ConflictException("A phone recipient address is required.");
        }
    }
}
