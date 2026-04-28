package SA.irms.common.messaging;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcProcessedEventRepository implements ProcessedEventRepository {
    private final JdbcClient jdbcClient;

    JdbcProcessedEventRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void delete(UUID eventId, String consumerName) {
        jdbcClient.sql("""
                        delete from processed_events
                        where event_id = :eventId and consumer_name = :consumerName
                        """)
                .param("eventId", eventId)
                .param("consumerName", consumerName)
                .update();
    }
}
