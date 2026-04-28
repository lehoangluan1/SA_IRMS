package SA.irms.common.inbox;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.common.events.EventEnvelope;

@Repository("processedEventRepository")
public class ProcessedEventRepository {
    private final JdbcClient jdbcClient;

    public ProcessedEventRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<ProcessedEvent> find(UUID eventId, String consumerName) {
        return jdbcClient.sql("""
                        select event_id, consumer_name, event_type, processed_at, status, error
                        from processed_events
                        where event_id = :eventId and consumer_name = :consumerName
                        """)
                .param("eventId", eventId)
                .param("consumerName", consumerName)
                .query((rs, rowNum) -> new ProcessedEvent(
                        rs.getObject("event_id", UUID.class),
                        rs.getString("consumer_name"),
                        rs.getString("event_type"),
                        rs.getTimestamp("processed_at") == null ? null : rs.getTimestamp("processed_at").toInstant(),
                        rs.getString("status"),
                        rs.getString("error")
                ))
                .optional();
    }

    /**
     * Claims an event for one consumer. FAILED rows are claimable again for safe retry; PROCESSED rows are immutable.
     */
    public boolean tryStart(EventEnvelope envelope, String consumerName) {
        int claimed = jdbcClient.sql("""
                        insert into processed_events (
                            event_id,
                            consumer_name,
                            event_type,
                            status,
                            processed_at,
                            error,
                            correlation_id,
                            causation_id,
                            retry_count,
                            next_retry_at
                        ) values (
                            :eventId,
                            :consumerName,
                            :eventType,
                            'IN_PROGRESS',
                            now(),
                            null,
                            :correlationId,
                            :causationId,
                            0,
                            null
                        )
                        on conflict (event_id, consumer_name) do update
                        set status = 'IN_PROGRESS',
                            processed_at = now(),
                            error = null,
                            correlation_id = excluded.correlation_id,
                            causation_id = excluded.causation_id,
                            next_retry_at = null
                        where processed_events.status <> 'PROCESSED'
                        """)
                .param("eventId", envelope.metadata().eventId())
                .param("consumerName", consumerName)
                .param("eventType", envelope.metadata().eventType())
                .param("correlationId", envelope.metadata().correlationId())
                .param("causationId", envelope.metadata().causationId())
                .update();
        return claimed == 1;
    }

    public void markProcessed(UUID eventId, String consumerName) {
        jdbcClient.sql("""
                        update processed_events
                        set status = 'PROCESSED', processed_at = now(), error = null, next_retry_at = null
                        where event_id = :eventId and consumer_name = :consumerName
                        """)
                .param("eventId", eventId)
                .param("consumerName", consumerName)
                .update();
    }

    public void markFailed(UUID eventId, String consumerName, Throwable failure) {
        jdbcClient.sql("""
                        update processed_events
                        set status = 'FAILED',
                            processed_at = now(),
                            error = :error,
                            retry_count = retry_count + 1,
                            next_retry_at = :nextRetryAt
                        where event_id = :eventId and consumer_name = :consumerName
                        """)
                .param("eventId", eventId)
                .param("consumerName", consumerName)
                .param("error", truncate(failure == null ? "Unknown consumer failure." : failure.getMessage()))
                .param("nextRetryAt", java.sql.Timestamp.from(Instant.now().plus(Duration.ofSeconds(30))))
                .update();
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown consumer failure.";
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
