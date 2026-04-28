package SA.irms.notification.application.port.out;

import SA.irms.common.notification.NotificationCommand;

public interface NotificationChannelAdapter {
    String channel();

    void validate(NotificationCommand command);
}
