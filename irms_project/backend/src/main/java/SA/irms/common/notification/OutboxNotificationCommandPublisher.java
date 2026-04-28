package SA.irms.common.notification;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import SA.irms.common.outbox.OutboxEventPublisher;
import SA.irms.common.events.ServiceEventTypes;
import SA.irms.common.notification.NotificationCommand;
import SA.irms.common.notification.NotificationCommandPublisher;

@Service
public class OutboxNotificationCommandPublisher implements NotificationCommandPublisher {
    private final OutboxEventPublisher outboxEventPublisher;

    public OutboxNotificationCommandPublisher(OutboxEventPublisher outboxEventPublisher) {
        this.outboxEventPublisher = outboxEventPublisher;
    }

    @Override
    public void enqueue(NotificationCommand command, String aggregateType, String aggregateId, String correlationId) {
        outboxEventPublisher.publish(ServiceEventTypes.NOTIFICATION_REQUESTED, aggregateType, aggregateId, toPayload(command), correlationId);
    }

    private Map<String, Object> toPayload(NotificationCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        put(payload, "lowStockAlertId", command.lowStockAlertId());
        put(payload, "reservationId", command.reservationId());
        put(payload, "waitlistEntryId", command.waitlistEntryId());
        put(payload, "paymentId", command.paymentId());
        put(payload, "channel", command.channel());
        put(payload, "type", command.type());
        put(payload, "templateCode", command.templateCode());
        payload.put("payload", command.payload() == null ? Map.of() : command.payload());
        put(payload, "title", command.title());
        put(payload, "body", command.body());
        put(payload, "recipientRole", command.recipientRole());
        put(payload, "recipientUserId", command.recipientUserId());
        put(payload, "recipientAddress", command.recipientAddress());
        put(payload, "priority", command.priority());
        return payload;
    }

    private void put(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value.toString());
        }
    }
}
