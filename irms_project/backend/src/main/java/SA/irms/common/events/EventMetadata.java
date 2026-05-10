package SA.irms.common.events;

import java.time.Instant;
import java.util.UUID;

public record EventMetadata(
        UUID eventId,
        String eventType,
        int eventVersion,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        String producerService,
        String correlationId,
        String causationId,
        String idempotencyKey
) {
    public EventMetadata {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId is required.");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType is required.");
        }
        if (eventVersion < 1) {
            throw new IllegalArgumentException("eventVersion must be >= 1.");
        }
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException("aggregateType is required.");
        }
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("aggregateId is required.");
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        producerService = normalize(producerService, "irms-service");
        correlationId = normalize(correlationId, eventId.toString());
        causationId = normalize(causationId, correlationId);
        idempotencyKey = normalize(idempotencyKey, eventType + ":" + aggregateType + ":" + aggregateId + ":" + correlationId);
    }

    public static EventMetadata create(DomainEvent event, String producerService, String correlationId, String causationId, String idempotencyKey) {
        return new EventMetadata(
                UUID.randomUUID(),
                event.eventType(),
                event.eventVersion(),
                event.aggregateType(),
                event.aggregateId(),
                Instant.now(),
                producerService,
                correlationId,
                causationId,
                idempotencyKey
        );
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
