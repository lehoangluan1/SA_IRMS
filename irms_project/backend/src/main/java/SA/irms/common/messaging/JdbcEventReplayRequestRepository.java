package SA.irms.common.messaging;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcEventReplayRequestRepository implements EventReplayRequestRepository {
    private final JdbcClient jdbcClient;

    JdbcEventReplayRequestRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void enqueueOutboxReplay(UUID eventId, String reason) {
        jdbcClient.sql("""
                        insert into event_replay_requests (replay_id, event_id, reason, status, requested_at)
                        values (:replayId, :eventId, :reason, 'QUEUED', now())
                        """)
                .param("replayId", UUID.randomUUID())
                .param("eventId", eventId)
                .param("reason", reason)
                .update();
    }

    @Override
    public void enqueueConsumerReplay(UUID eventId, String consumerName, String reason) {
        jdbcClient.sql("""
                        insert into event_replay_requests (replay_id, event_id, consumer_name, reason, status, requested_at)
                        values (:replayId, :eventId, :consumerName, :reason, 'QUEUED', now())
                        """)
                .param("replayId", UUID.randomUUID())
                .param("eventId", eventId)
                .param("consumerName", consumerName)
                .param("reason", reason)
                .update();
    }
}
