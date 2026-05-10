package SA.irms.notification.application.port.in;

import SA.irms.common.events.EventEnvelope;

public interface NotificationRequestConsumerUseCase {
    void materialize(EventEnvelope envelope);
}
