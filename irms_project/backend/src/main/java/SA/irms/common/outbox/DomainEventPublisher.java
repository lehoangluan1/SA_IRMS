package SA.irms.common.outbox;

import java.util.Map;
import java.util.UUID;

import SA.irms.common.events.DomainEvent;
import SA.irms.common.events.EventMetadata;

public interface DomainEventPublisher {
    UUID publish(DomainEvent event, String correlationId, String causationId);

    UUID publish(String eventType, int eventVersion, String aggregateType, String aggregateId,
                 Map<String, Object> payload, String correlationId, String causationId, String producerService);

    UUID publish(EventMetadata metadata, Map<String, Object> payload);
}
