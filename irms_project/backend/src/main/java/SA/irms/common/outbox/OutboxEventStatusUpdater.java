package SA.irms.common.outbox;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class OutboxEventStatusUpdater {
    private final JdbcClient jdbcClient;
    private final Clock clock;

    OutboxEventStatusUpdater(JdbcClient jdbcClient, Clock clock) {
        this.jdbcClient = jdbcClient;
        this.clock = clock;
    }

    void markPublished(UUID eventId) {
        jdbcClient.sql("""
                        update outbox_events
                        set status = 'PUBLISHED',
                            published_at = now(),
                            last_error = null
                        where event_id = :eventId
                        """)
                .param("eventId", eventId)
                .update();
    }

    void markFailed(UUID eventId, Throwable failure, int maxRetries, Duration baseBackoff) {
        String message = failure == null ? "Unknown RabbitMQ publish failure." : failure.getMessage();
        int retryCount = jdbcClient.sql("select retry_count from outbox_events where event_id = :eventId")
                .param("eventId", eventId)
                .query(Integer.class)
                .optional()
                .orElse(0) + 1;
        boolean terminal = retryCount >= maxRetries;
        Instant nextRetryAt = Instant.now(clock).plus(baseBackoff.multipliedBy(Math.max(1, retryCount)));
        jdbcClient.sql("""
                        update outbox_events
                        set retry_count = retry_count + 1,
                            status = :status,
                            next_retry_at = :nextRetryAt,
                            last_error = :lastError
                        where event_id = :eventId
                        """)
                .param("status", terminal ? "FAILED" : "PENDING")
                .param("nextRetryAt", java.sql.Timestamp.from(nextRetryAt))
                .param("lastError", truncate(message))
                .param("eventId", eventId)
                .update();
    }

    void resetForReplay(UUID eventId) {
        jdbcClient.sql("""
                        update outbox_events
                        set status = 'PENDING',
                            retry_count = 0,
                            next_retry_at = now(),
                            last_error = null
                        where event_id = :eventId
                        """)
                .param("eventId", eventId)
                .update();
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown outbox error.";
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
