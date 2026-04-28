package SA.irms.identity.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rabbitmq.client.Channel;

import SA.irms.identity.persistence.IdentityRepository;
import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventConsumers;
import SA.irms.common.messaging.ManualAckConsumerSupport;

@Component
@ConditionalOnProperty(name = "irms.rabbitmq.enabled", havingValue = "true")
public class SessionTouchRequestedConsumer {
    private final ManualAckConsumerSupport ackSupport;
    private final IdentityRepository identityRepository;

    public SessionTouchRequestedConsumer(ManualAckConsumerSupport ackSupport, IdentityRepository identityRepository) {
        this.ackSupport = ackSupport;
        this.identityRepository = identityRepository;
    }

    @RabbitListener(queues = "irms.audit.session-touch.q")
    public void consume(Message message, Channel channel) throws java.io.IOException {
        ackSupport.handle(message, channel, ServiceEventConsumers.SESSION_TOUCH_MATERIALIZER, this::touchSession);
    }

    @Transactional
    void touchSession(EventEnvelope envelope) {
        UUID sessionId = UUID.fromString(text(envelope.payload().get("sessionId"), envelope.metadata().aggregateId()));
        Instant lastActivityAt = Instant.parse(text(envelope.payload().get("lastActivityAt"), envelope.metadata().occurredAt().toString()));
        identityRepository.touchSession(sessionId, lastActivityAt);
    }

    private String text(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }
}
