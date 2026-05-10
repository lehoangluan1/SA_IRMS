package SA.irms.notification.infrastructure.messaging;

import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventConsumers;
import SA.irms.common.messaging.ManualAckConsumerSupport;
import SA.irms.common.notification.NotificationCommand;
import SA.irms.common.notification.NotificationCommandPublisher;

@Component
@ConditionalOnProperty(name = "irms.rabbitmq.enabled", havingValue = "true")
public class AuditFollowUpRequestedConsumer {
    private final ManualAckConsumerSupport ackSupport;
    private final NotificationCommandPublisher notificationOutboxPublisher;

    public AuditFollowUpRequestedConsumer(ManualAckConsumerSupport ackSupport, NotificationCommandPublisher notificationOutboxPublisher) {
        this.ackSupport = ackSupport;
        this.notificationOutboxPublisher = notificationOutboxPublisher;
    }

    @RabbitListener(queues = "irms.notification.audit-follow-up.q")
    public void consume(Message message, Channel channel) throws java.io.IOException {
        ackSupport.handle(message, channel, ServiceEventConsumers.AUDIT_FOLLOW_UP_ROUTER, this::routeFollowUp);
    }

    void routeFollowUp(EventEnvelope envelope) {
        for (String role : List.of("manager", "admin")) {
            notificationOutboxPublisher.enqueue(new NotificationCommand(
                    null,
                    null,
                    null,
                    null,
                    "in_app",
                    "audit_follow_up",
                    "AUDIT_FOLLOW_UP_REQUIRED",
                    Map.of(
                            "auditLogId", string(envelope.payload().get("auditLogId")),
                            "action", string(envelope.payload().get("action")),
                            "entityType", string(envelope.payload().get("entityType")),
                            "entityId", string(envelope.payload().get("entityId"))
                    ),
                    "Audit follow-up required",
                    "Review required for " + string(envelope.payload().get("action")) + ".",
                    role,
                    null,
                    role,
                    "high"
            ), "AuditLog", envelope.metadata().aggregateId(), envelope.metadata().correlationId());
        }
    }

    private String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
