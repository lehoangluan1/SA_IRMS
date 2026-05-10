package SA.irms.common.messaging;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcDeadLetterEventRepository implements DeadLetterEventRepository {
    private final JdbcClient jdbcClient;

    JdbcDeadLetterEventRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void record(DeadLetterEventRecord record) {
        jdbcClient.sql("""
                        insert into dead_letter_events (
                            dead_letter_id,
                            event_id,
                            consumer_name,
                            event_type,
                            payload,
                            broker_headers,
                            failure_reason,
                            correlation_id,
                            causation_id,
                            status,
                            created_at
                        ) values (
                            :deadLetterId,
                            :eventId,
                            :consumerName,
                            :eventType,
                            cast(:payload as jsonb),
                            cast(:brokerHeaders as jsonb),
                            :failureReason,
                            :correlationId,
                            :causationId,
                            'FAILED',
                            now()
                        )
                        """)
                .param("deadLetterId", record.deadLetterId())
                .param("eventId", record.eventId())
                .param("consumerName", record.consumerName())
                .param("eventType", record.eventType())
                .param("payload", record.payloadJson())
                .param("brokerHeaders", record.brokerHeadersJson())
                .param("failureReason", record.failureReason())
                .param("correlationId", record.correlationId())
                .param("causationId", record.causationId())
                .update();
    }

    @Override
    public void markReplayRequested(UUID eventId, String consumerName) {
        jdbcClient.sql("""
                        update dead_letter_events
                        set status = 'REPLAY_REQUESTED',
                            replay_count = replay_count + 1
                        where event_id = :eventId
                          and consumer_name = :consumerName
                          and status = 'FAILED'
                        """)
                .param("eventId", eventId)
                .param("consumerName", consumerName)
                .update();
    }
}
