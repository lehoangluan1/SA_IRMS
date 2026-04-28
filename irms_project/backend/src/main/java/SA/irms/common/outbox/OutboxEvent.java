package SA.irms.common.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OutboxEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        String aggregateType,
        String aggregateId,
        Map<String, Object> payload,
        String correlationId,
        String causationId,
        String producerService,
        String idempotencyKey,
        String routingKey,
        String exchangeName,
        String status,
        int retryCount,
        Instant nextRetryAt,
        Instant occurredAt,
        Instant createdAt,
        Instant publishedAt,
        String lastError
) {
    public OutboxEvent {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
