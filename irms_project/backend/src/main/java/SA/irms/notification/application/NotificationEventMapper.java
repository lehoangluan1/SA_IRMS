package SA.irms.notification.application;

import java.util.Optional;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.notification.NotificationCommand;

public interface NotificationEventMapper {
    boolean supports(EventEnvelope envelope);

    Optional<NotificationCommand> map(EventEnvelope envelope);
}
