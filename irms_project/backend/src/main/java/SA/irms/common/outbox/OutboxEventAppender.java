package SA.irms.common.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.common.events.EventMetadata;
import SA.irms.common.events.EventRouting;

@Repository
class OutboxEventAppender {
    private final JdbcClient jdbcClient;
    private final OutboxEventPayloadMapper payloadMapper;
    private final Clock clock;

    OutboxEventAppender(JdbcClient jdbcClient, OutboxEventPayloadMapper payloadMapper, Clock clock) {
        this.jdbcClient = jdbcClient;
        this.payloadMapper = payloadMapper;
        this.clock = clock;
    }

    UUID storePending(EventMetadata metadata, Map<String, Object> payload, EventRouting routing) {
        UUID eventId = metadata.eventId();
        int updated = jdbcClient.sql("""
                        insert into outbox_events (
                            event_id,
                            type,
                            event_type,
                            event_version,
                            aggregate_type,
                            aggregate_id,
                            payload,
                            correlation_id,
                            causation_id,
                            producer_service,
                            idempotency_key,
                            routing_key,
                            exchange_name,
                            status,
                            retry_count,
                            next_retry_at,
                            occurred_at,
                            created_at
                        ) values (
                            :eventId,
                            :eventType,
                            :eventType,
                            :eventVersion,
                            :aggregateType,
                            :aggregateId,
                            cast(:payload as jsonb),
                            :correlationId,
                            :causationId,
                            :producerService,
                            :idempotencyKey,
                            :routingKey,
                            :exchangeName,
                            'PENDING',
                            0,
                            :nextRetryAt,
                            :occurredAt,
                            :createdAt
                        )
                        on conflict (idempotency_key) where idempotency_key is not null do nothing
                        """)
                .param("eventId", eventId)
                .param("eventType", metadata.eventType())
                .param("eventVersion", metadata.eventVersion())
                .param("aggregateType", metadata.aggregateType())
                .param("aggregateId", metadata.aggregateId())
                .param("payload", payloadMapper.toJson(payload))
                .param("correlationId", metadata.correlationId())
                .param("causationId", metadata.causationId())
                .param("producerService", metadata.producerService())
                .param("idempotencyKey", metadata.idempotencyKey())
                .param("routingKey", routing.routingKey())
                .param("exchangeName", routing.exchangeName())
                .param("nextRetryAt", java.sql.Timestamp.from(Instant.now(clock)))
                .param("occurredAt", java.sql.Timestamp.from(metadata.occurredAt()))
                .param("createdAt", java.sql.Timestamp.from(Instant.now(clock)))
                .update();
        if (updated == 1 || metadata.idempotencyKey() == null || metadata.idempotencyKey().isBlank()) {
            return eventId;
        }
        return findEventIdByIdempotencyKey(metadata.idempotencyKey())
                .orElseThrow(() -> new IllegalStateException("Outbox event id could not be resolved from idempotency key."));
    }

    Optional<UUID> findEventIdByIdempotencyKey(String idempotencyKey) {
        return jdbcClient.sql("select event_id from outbox_events where idempotency_key = :idempotencyKey")
                .param("idempotencyKey", idempotencyKey)
                .query(UUID.class)
                .optional();
    }
}
