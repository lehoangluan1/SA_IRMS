package SA.irms.common.notification;

public interface NotificationCommandPublisher {
    void enqueue(NotificationCommand command, String aggregateType, String aggregateId, String correlationId);
}
