package SA.irms.reservation.infrastructure.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.reservation.application.port.out.ReservationSeatingCommandPort;

@Repository
public class JdbcReservationSeatingAdapter implements ReservationSeatingCommandPort {
    private final JdbcClient jdbcClient;

    public JdbcReservationSeatingAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean canSeatReservation(UUID reservationId, UUID tableId) {
        Long count = jdbcClient.sql("""
                        select count(*)
                        from reservations r
                        join dining_tables t on t.table_id = :tableId
                        where r.reservation_id = :reservationId
                          and r.status in ('confirmed', 'arrived')
                          and t.status in ('available', 'reserved')
                        """)
                .param("reservationId", reservationId)
                .param("tableId", tableId)
                .query(Long.class)
                .single();
        return count != null && count > 0;
    }

    @Override
    public void assignTable(UUID reservationId, UUID tableId) {
        jdbcClient.sql("""
                        update reservations
                        set table_id = :tableId, updated_at = now()
                        where reservation_id = :reservationId
                        """)
                .param("reservationId", reservationId)
                .param("tableId", tableId)
                .update();
        jdbcClient.sql("""
                        update dining_tables
                        set status = 'reserved', updated_at = now()
                        where table_id = :tableId and status = 'available'
                        """)
                .param("tableId", tableId)
                .update();
    }

    @Override
    public void seatReservation(UUID reservationId, UUID tableId) {
        jdbcClient.sql("""
                        update reservations
                        set status = 'seated', updated_at = now()
                        where reservation_id = :reservationId and status in ('confirmed', 'arrived')
                        """)
                .param("reservationId", reservationId)
                .update();
        jdbcClient.sql("""
                        update dining_tables
                        set status = 'occupied', updated_at = now()
                        where table_id = :tableId
                        """)
                .param("tableId", tableId)
                .update();
    }

    @Override
    public void updateWaitlistStatus(UUID waitlistEntryId, String status) {
        jdbcClient.sql("""
                        update waitlist_entries
                        set status = :status, updated_at = now()
                        where waitlist_entry_id = :waitlistEntryId
                        """)
                .param("waitlistEntryId", waitlistEntryId)
                .param("status", status)
                .update();
    }
}
