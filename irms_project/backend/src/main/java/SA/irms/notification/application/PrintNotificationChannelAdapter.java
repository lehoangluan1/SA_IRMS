package SA.irms.notification.application;

import org.springframework.stereotype.Component;

import SA.irms.notification.application.port.out.PrintNotificationChannel;
import SA.irms.common.notification.NotificationCommand;

@Component
public class PrintNotificationChannelAdapter implements PrintNotificationChannel {
    @Override
    public String channel() {
        return "print";
    }

    @Override
    public void validate(NotificationCommand command) {
        // Printing can be queued without a recipient address.
    }
}
