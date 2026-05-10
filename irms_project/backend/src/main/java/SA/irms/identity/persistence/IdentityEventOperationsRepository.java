package SA.irms.identity.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.identity.application.port.out.IdentityEventOperationsPort;

@Repository
public class IdentityEventOperationsRepository implements IdentityEventOperationsPort {
    private final JdbcClient jdbcClient;

    public IdentityEventOperationsRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<IdentityEventOperationsPort.FailedOutboxEventRow> failedOutboxEvents() {
        return jdbcClient.sql("""
                        select event_id, coalesce(event_type, type) as event_type, aggregate_type, aggregate_id,
                               producer_service, routing_key, exchange_name, retry_count, last_error, created_at
                        from outbox_events
                        where status in ('FAILED', 'failed')
                        order by created_at desc
                        limit 100
                        """)
                .query((rs, rowNum) -> new IdentityEventOperationsPort.FailedOutboxEventRow(
                        rs.getObject("event_id", UUID.class),
                        rs.getString("event_type"),
                        rs.getString("aggregate_type"),
                        rs.getString("aggregate_id"),
                        rs.getString("producer_service"),
                        rs.getString("exchange_name"),
                        rs.getString("routing_key"),
                        rs.getInt("retry_count"),
                        rs.getString("last_error"),
                        rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toInstant()
                ))
                .list();
    }

    @Override
    public List<IdentityEventOperationsPort.DeadLetterEventRow> deadLetterEvents() {
        return jdbcClient.sql("""
                        select dead_letter_id, event_id, consumer_name, event_type, failure_reason, status, created_at
                        from dead_letter_events
                        order by created_at desc
                        limit 100
                        """)
                .query((rs, rowNum) -> new IdentityEventOperationsPort.DeadLetterEventRow(
                        rs.getObject("dead_letter_id", UUID.class),
                        rs.getObject("event_id", UUID.class),
                        rs.getString("consumer_name"),
                        rs.getString("event_type"),
                        rs.getString("failure_reason"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toInstant()
                ))
                .list();
    }

}