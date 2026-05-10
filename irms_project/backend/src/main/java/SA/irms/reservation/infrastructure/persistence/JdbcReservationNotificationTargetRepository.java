package SA.irms.reservation.infrastructure.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.common.error.NotFoundException;
import SA.irms.reservation.application.port.out.ReservationNotificationTargetRepository;
import SA.irms.reservation.application.query.ReservationNotificationTarget;

@Repository
public class JdbcReservationNotificationTargetRepository implements ReservationNotificationTargetRepository {
    private final JdbcClient jdbcClient;

    public JdbcReservationNotificationTargetRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public ReservationNotificationTarget loadReservationNotificationTarget(UUID reservationId) {
        return jdbcClient.sql("""
                        select c.name,
                               c.phone,
                               c.email
                        from reservations r
                        join reservation_contacts c on c.contact_id = r.contact_id
                        where r.reservation_id = :reservationId
                        """)
                .param("reservationId", reservationId)
                .query((rs, rowNum) -> new ReservationNotificationTarget(
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("email")
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Reservation contact details were not found."));
    }

    @Override
    public ReservationNotificationTarget loadWaitlistNotificationTarget(UUID waitlistEntryId) {
        return jdbcClient.sql("""
                        select contact_name,
                               contact_phone
                        from waitlist_entries
                        where waitlist_entry_id = :waitlistEntryId
                        """)
                .param("waitlistEntryId", waitlistEntryId)
                .query((rs, rowNum) -> new ReservationNotificationTarget(
                        rs.getString("contact_name"),
                        rs.getString("contact_phone"),
                        null
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Waitlist contact details were not found."));
    }
}
