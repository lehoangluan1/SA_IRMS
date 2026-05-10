package SA.irms.common.outbox;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.outbox.DomainEventPublisher;

/**
 * Compatibility adapter for older application components that still depend on the
 * common OutboxEventPublisher port. It writes into the canonical RabbitMQ
 * transactional outbox through DomainEventPublisher and never creates legacy
 * DB fanout deliveries. The old database fanout bridge has been removed from active release-candidate source wiring and remains disabled by default via configuration.
 */
@Component
@Profile("!api-gateway")
public class JdbcOutboxEventPublisher implements OutboxEventPublisher {
    private final DomainEventPublisher canonicalPublisher;

    public JdbcOutboxEventPublisher(DomainEventPublisher canonicalPublisher) {
        this.canonicalPublisher = canonicalPublisher;
    }

    @Override
    @Transactional
    public void publish(String type, String aggregateType, String aggregateId, Map<String, Object> payload, String correlationId) {
        canonicalPublisher.publish(
                type,
                1,
                aggregateType,
                aggregateId,
                payload == null ? Map.of() : payload,
                correlationId,
                correlationId,
                "legacy-compatibility-adapter"
        );
    }
}
