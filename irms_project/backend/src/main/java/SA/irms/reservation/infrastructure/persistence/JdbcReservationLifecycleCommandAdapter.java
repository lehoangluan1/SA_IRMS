package SA.irms.reservation.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.reservation.application.support.ReservationTimeSupport;
import SA.irms.reservation.application.port.out.ReservationLifecycleCommandPort;

@Repository
public class JdbcReservationLifecycleCommandAdapter implements ReservationLifecycleCommandPort {
    private final JdbcClient jdbcClient;

    public JdbcReservationLifecycleCommandAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public UUID createContact(String name, String phone, String email) {
        UUID contactId = UUID.randomUUID();
        jdbcClient.sql("""
                        insert into reservation_contacts (contact_id, name, phone, email, notification_channel)
                        values (:contactId, :name, :phone, :email, 'sms')
                        """)
                .param("contactId", contactId)
                .param("name", name)
                .param("phone", phone)
                .param("email", email)
                .update();
        return contactId;
    }

    @Override
    public UUID createReservation(UUID branchId, UUID contactId, Instant arrivalAt, int partySize, String status, String notes,
                                  Instant confirmedAt, Instant holdExpiresAt) {
        UUID reservationId = UUID.randomUUID();
        jdbcClient.sql("""
                        insert into reservations (
                            reservation_id,
                            branch_id,
                            contact_id,
                            arrival_at,
                            party_size,
                            status,
                            source,
                            notes,
                            confirmed_at,
                            hold_expires_at
                        ) values (
                            :reservationId,
                            :branchId,
                            :contactId,
                            :arrivalAt,
                            :partySize,
                            :status,
                            'manual',
                            :notes,
                            :confirmedAt,
                            :holdExpiresAt
                        )
                        """)
                .param("reservationId", reservationId)
                .param("branchId", branchId)
                .param("contactId", contactId)
                .param("arrivalAt", ReservationTimeSupport.toSqlTimestamp(arrivalAt))
                .param("partySize", partySize)
                .param("status", status)
                .param("notes", notes)
                .param("confirmedAt", ReservationTimeSupport.toSqlTimestamp(confirmedAt))
                .param("holdExpiresAt", ReservationTimeSupport.toSqlTimestamp(holdExpiresAt))
                .update();
        return reservationId;
    }

    @Override
    public void updateReservation(UUID reservationId, String notes, int partySize) {
        jdbcClient.sql("""
                        update reservations
                        set notes = :notes,
                            party_size = :partySize,
                            updated_at = now()
                        where reservation_id = :reservationId
                        """)
                .param("notes", notes)
                .param("partySize", partySize)
                .param("reservationId", reservationId)
                .update();
    }

    @Override
    public void updateContactNameForReservation(UUID reservationId, String name) {
        jdbcClient.sql("""
                        update reservation_contacts
                        set name = :name
                        where contact_id = (select contact_id from reservations where reservation_id = :reservationId)
                        """)
                .param("name", name)
                .param("reservationId", reservationId)
                .update();
    }

    @Override
    public void confirmReservation(UUID reservationId) {
        jdbcClient.sql("""
                        update reservations
                        set status = 'confirmed',
                            confirmed_at = now(),
                            updated_at = now()
                        where reservation_id = :reservationId
                        """)
                .param("reservationId", reservationId)
                .update();
    }

    @Override
    public void markReservationNoShow(UUID reservationId) {
        jdbcClient.sql("""
                        update reservations
                        set status = 'no_show',
                            updated_at = now()
                        where reservation_id = :reservationId
                        """)
                .param("reservationId", reservationId)
                .update();
    }

    @Override
    public List<UUID> loadOverdueReservationIds(Instant cutoff) {
        return jdbcClient.sql("""
                        select reservation_id
                        from reservations
                        where status in ('pending', 'confirmed')
                          and arrival_at <= :cutoff
                        """)
                .param("cutoff", ReservationTimeSupport.toSqlTimestamp(cutoff))
                .query(UUID.class)
                .list();
    }

    @Override
    public void cancelExpiredReservation(UUID reservationId, String cancelReason) {
        jdbcClient.sql("""
                        update reservations
                        set status = 'cancelled',
                            cancelled_at = now(),
                            cancel_reason = :cancelReason,
                            updated_at = now()
                        where reservation_id = :reservationId
                        """)
                .param("reservationId", reservationId)
                .param("cancelReason", cancelReason)
                .update();
    }

    @Override
    public List<UUID> loadUpcomingReminderReservationIds(Instant windowStart, Instant windowEnd, Instant windowFloor) {
        return jdbcClient.sql("""
                        select r.reservation_id
                        from reservations r
                        where r.status = 'confirmed'
                          and r.arrival_at between :windowStart and :windowEnd
                          and not exists (
                              select 1
                              from notification_messages nm
                              where nm.reservation_id = r.reservation_id
                                and nm.type = 'reservation_check_in_time'
                                and nm.created_at >= :windowFloor
                          )
                        """)
                .param("windowStart", ReservationTimeSupport.toSqlTimestamp(windowStart))
                .param("windowEnd", ReservationTimeSupport.toSqlTimestamp(windowEnd))
                .param("windowFloor", ReservationTimeSupport.toSqlTimestamp(windowFloor))
                .query(UUID.class)
                .list();
    }
}
