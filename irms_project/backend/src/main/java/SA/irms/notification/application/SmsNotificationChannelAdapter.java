package SA.irms.notification.application;

import org.springframework.stereotype.Component;

import SA.irms.notification.application.port.out.SmsNotificationChannel;

import SA.irms.common.error.ConflictException;
import SA.irms.common.notification.NotificationCommand;

@Component
public class SmsNotificationChannelAdapter implements SmsNotificationChannel {
    @Override
    public String channel() {
        return "sms";
    }

    @Override
    public void validate(NotificationCommand command) {
        if (command.recipientAddress() == null || command.recipientAddress().isBlank()) {
            throw new ConflictException("An SMS recipient address is required.");
        }
    }
}
