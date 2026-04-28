package SA.irms.reservation.infrastructure.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.reservation.application.port.out.ReservationCheckInCommandPort;

@Repository
public class JdbcReservationCheckInCommandAdapter implements ReservationCheckInCommandPort {
    private final JdbcClient jdbcClient;

    public JdbcReservationCheckInCommandAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public UUID openReservationSession(UUID reservationId, UUID tableId, UUID serverUserId, int guestCount, String correlationId) {
        UUID sessionId = UUID.randomUUID();
        jdbcClient.sql("""
                        insert into table_sessions (
                            session_id,
                            table_id,
                            reservation_id,
                            server_user_id,
                            guest_count,
                            status,
                            correlation_id,
                            opened_at
                        ) values (
                            :sessionId,
                            :tableId,
                            :reservationId,
                            :serverUserId,
                            :guestCount,
                            'active',
                            :correlationId,
                            now()
                        )
                        """)
                .param("sessionId", sessionId)
                .param("tableId", tableId)
                .param("reservationId", reservationId)
                .param("serverUserId", serverUserId)
                .param("guestCount", guestCount)
                .param("correlationId", correlationId)
                .update();
        return sessionId;
    }

    @Override
    public void markTableOccupied(UUID tableId) {
        jdbcClient.sql("""
                        update dining_tables
                        set status = 'occupied',
                            updated_at = now()
                        where table_id = :tableId
                        """)
                .param("tableId", tableId)
                .update();
    }

    @Override
    public void markReservationSeated(UUID reservationId, int actualPartySize) {
        jdbcClient.sql("""
                        update reservations
                        set status = 'seated',
                            party_size = :partySize,
                            checked_in_at = now(),
                            updated_at = now()
                        where reservation_id = :reservationId
                        """)
                .param("partySize", actualPartySize)
                .param("reservationId", reservationId)
                .update();
    }

    @Override
    public void linkReservationAssignmentSession(UUID reservationId, UUID tableId, UUID sessionId) {
        jdbcClient.sql("""
                        update table_assignments
                        set table_session_id = :sessionId,
                            updated_at = now()
                        where reservation_id = :reservationId
                          and table_id = :tableId
                          and released_at is null
                        """)
                .param("sessionId", sessionId)
                .param("reservationId", reservationId)
                .param("tableId", tableId)
                .update();
    }
}
