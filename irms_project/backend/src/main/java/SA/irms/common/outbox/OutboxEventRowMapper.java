package SA.irms.common.outbox;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
class OutboxEventRowMapper implements RowMapper<OutboxEvent> {
    private final OutboxEventPayloadMapper payloadMapper;

    OutboxEventRowMapper(OutboxEventPayloadMapper payloadMapper) {
        this.payloadMapper = payloadMapper;
    }

    @Override
    public OutboxEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new OutboxEvent(
                rs.getObject("event_id", java.util.UUID.class),
                rs.getString("event_type"),
                rs.getInt("event_version"),
                rs.getString("aggregate_type"),
                rs.getString("aggregate_id"),
                payloadMapper.readPayload(rs.getString("payload")),
                rs.getString("correlation_id"),
                rs.getString("causation_id"),
                rs.getString("producer_service"),
                rs.getString("idempotency_key"),
                rs.getString("routing_key"),
                rs.getString("exchange_name"),
                rs.getString("status"),
                rs.getInt("retry_count"),
                toInstant(rs.getTimestamp("next_retry_at")),
                toInstant(rs.getTimestamp("occurred_at")),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("published_at")),
                rs.getString("last_error")
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
