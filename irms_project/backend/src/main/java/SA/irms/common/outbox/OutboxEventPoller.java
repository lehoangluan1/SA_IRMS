package SA.irms.common.outbox;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class OutboxEventPoller {
    private final JdbcClient jdbcClient;
    private final OutboxEventRowMapper rowMapper;

    OutboxEventPoller(JdbcClient jdbcClient, OutboxEventRowMapper rowMapper) {
        this.jdbcClient = jdbcClient;
        this.rowMapper = rowMapper;
    }

    List<OutboxEvent> lockPendingForPublish(int limit) {
        return jdbcClient.sql("""
                        select event_id,
                               coalesce(event_type, type) as event_type,
                               coalesce(event_version, 1) as event_version,
                               aggregate_type,
                               aggregate_id,
                               payload::text as payload,
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
                               created_at,
                               published_at,
                               last_error
                        from outbox_events
                        where status = 'PENDING'
                          and coalesce(next_retry_at, now()) <= now()
                        order by occurred_at, created_at
                        limit :limit
                        for update skip locked
                        """)
                .param("limit", limit)
                .query(rowMapper)
                .list();
    }

    Optional<OutboxEvent> find(UUID eventId) {
        return jdbcClient.sql("""
                        select event_id,
                               coalesce(event_type, type) as event_type,
                               coalesce(event_version, 1) as event_version,
                               aggregate_type,
                               aggregate_id,
                               payload::text as payload,
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
                               created_at,
                               published_at,
                               last_error
                        from outbox_events
                        where event_id = :eventId
                        """)
                .param("eventId", eventId)
                .query(rowMapper)
                .optional();
    }
}
