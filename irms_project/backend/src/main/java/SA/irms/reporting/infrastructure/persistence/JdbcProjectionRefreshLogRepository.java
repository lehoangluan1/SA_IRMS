package SA.irms.reporting.infrastructure.persistence;

import java.sql.Timestamp;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.common.events.EventEnvelope;

final class JdbcProjectionRefreshLogRepository {
    private final JdbcClient jdbcClient;
    private final ReportSnapshotPayloadMapper payloadMapper;

    JdbcProjectionRefreshLogRepository(JdbcClient jdbcClient, ReportSnapshotPayloadMapper payloadMapper) {
        this.jdbcClient = jdbcClient;
        this.payloadMapper = payloadMapper;
    }

    void recordEventProjection(EventEnvelope envelope) {
        jdbcClient.sql("""
                        insert into reporting_event_projection (
                            event_id,
                            event_type,
                            aggregate_type,
                            aggregate_id,
                            payload,
                            occurred_at,
                            projected_at
                        ) values (
                            :eventId,
                            :eventType,
                            :aggregateType,
                            :aggregateId,
                            cast(:payload as jsonb),
                            :occurredAt,
                            now()
                        )
                        on conflict (event_id) do update
                        set projected_at = excluded.projected_at,
                            payload = excluded.payload
                        """)
                .param("eventId", envelope.metadata().eventId())
                .param("eventType", envelope.metadata().eventType())
                .param("aggregateType", envelope.metadata().aggregateType())
                .param("aggregateId", envelope.metadata().aggregateId())
                .param("payload", payloadMapper.eventPayloadToJson(envelope.payload()))
                .param("occurredAt", Timestamp.from(envelope.metadata().occurredAt()))
                .update();
    }

    boolean alreadyRefreshedForEvent(UUID outboxEventId) {
        if (outboxEventId == null) {
            return false;
        }
        return jdbcClient.sql("""
                        select count(*)
                        from report_snapshots
                        where payload ->> 'sourceEventId' = :outboxEventId
                        """)
                .param("outboxEventId", outboxEventId.toString())
                .query(Long.class)
                .single() > 0;
    }
}
