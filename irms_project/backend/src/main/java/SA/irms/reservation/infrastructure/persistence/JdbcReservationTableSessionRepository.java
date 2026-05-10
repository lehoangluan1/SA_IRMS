package SA.irms.reservation.infrastructure.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcReservationTableSessionRepository {
    private final JdbcClient jdbcClient;

    JdbcReservationTableSessionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    UUID createTableSession(UUID tableId, UUID serverUserId, int guestCount, String correlationId) {
        UUID sessionId = UUID.randomUUID();
        jdbcClient.sql("""
                        insert into table_sessions (
                            session_id,
                            table_id,
                            server_user_id,
                            guest_count,
                            status,
                            correlation_id,
                            opened_at
                        ) values (
                            :sessionId,
                            :tableId,
                            :serverUserId,
                            :guestCount,
                            'active',
                            :correlationId,
                            now()
                        )
                        """)
                .param("sessionId", sessionId)
                .param("tableId", tableId)
                .param("serverUserId", serverUserId)
                .param("guestCount", guestCount)
                .param("correlationId", correlationId)
                .update();
        return sessionId;
    }

    void markTableOccupied(UUID tableId) {
        jdbcClient.sql("""
                        update dining_tables
                        set status = 'occupied',
                            updated_at = now()
                        where table_id = :tableId
                        """)
                .param("tableId", tableId)
                .update();
    }
}
