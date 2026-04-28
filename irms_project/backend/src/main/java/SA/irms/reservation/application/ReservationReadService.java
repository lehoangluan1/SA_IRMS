package SA.irms.reservation.application;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import SA.irms.common.error.NotFoundException;
import SA.irms.reservation.application.support.ReservationTimeSupport;
import SA.irms.reservation.application.view.ReservationViews;

@Service
class ReservationReadService {
    private final JdbcClient jdbcClient;

    ReservationReadService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public ReservationViews.ReservationOverview load(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now(ZoneOffset.UTC) : date;
        List<ReservationViews.TableView> tables = jdbcClient.sql("""
                        select dt.table_id,
                               dt.code,
                               dt.capacity,
                               dt.status,
                               dt.zone,
                               dt.section_label,
                               ts.session_id,
                               ts.opened_at,
                               r.arrival_at
                        from dining_tables dt
                        left join table_sessions ts on ts.table_id = dt.table_id and ts.status in ('active', 'billing')
                        left join table_assignments ta on ta.table_id = dt.table_id and ta.released_at is null
                        left join reservations r on r.reservation_id = ta.reservation_id and r.status in ('confirmed', 'pending')
                        order by dt.code
                        """)
                .query((rs, rowNum) -> new ReservationViews.TableView(
                        rs.getObject("table_id", UUID.class),
                        SA.irms.common.support.TableCodeParser.parseTableNumber(rs.getString("code")),
                        rs.getInt("capacity"),
                        rs.getString("status"),
                        rs.getString("zone") + (rs.getString("section_label") == null ? "" : " - " + rs.getString("section_label")),
                        rs.getObject("session_id", UUID.class),
                        rs.getTimestamp("opened_at") == null ? null : ReservationTimeSupport.formatTime(rs.getTimestamp("opened_at").toInstant()),
                        rs.getTimestamp("arrival_at") == null ? null : ReservationTimeSupport.formatTime(rs.getTimestamp("arrival_at").toInstant())
                ))
                .list();

        List<ReservationViews.ReservationView> reservations = jdbcClient.sql("""
                        select r.reservation_id,
                               c.name,
                               c.phone,
                               c.email,
                               r.party_size,
                               r.arrival_at,
                               r.status,
                               r.notes,
                               dt.code as table_code
                        from reservations r
                        join reservation_contacts c on c.contact_id = r.contact_id
                        left join table_assignments ta on ta.reservation_id = r.reservation_id and ta.released_at is null
                        left join dining_tables dt on dt.table_id = ta.table_id
                        where date(r.arrival_at at time zone 'UTC') = :targetDate
                        order by r.arrival_at
                        """)
                .param("targetDate", targetDate)
                .query((rs, rowNum) -> new ReservationViews.ReservationView(
                        rs.getObject("reservation_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getInt("party_size"),
                        rs.getTimestamp("arrival_at").toLocalDateTime().toLocalDate().toString(),
                        ReservationTimeSupport.formatTime(rs.getTimestamp("arrival_at").toInstant()),
                        ReservationTimeSupport.mapReservationStatus(rs.getString("status")),
                        rs.getString("table_code") == null ? null : SA.irms.common.support.TableCodeParser.parseTableNumber(rs.getString("table_code")),
                        rs.getString("notes")
                ))
                .list();

        List<ReservationViews.WaitlistView> waitlist = jdbcClient.sql("""
                        select we.waitlist_entry_id,
                               we.contact_name,
                               we.contact_phone,
                               we.contact_email,
                               we.party_size,
                               we.quoted_wait_min,
                               we.status,
                               we.hold_expires_at,
                               we.notes,
                               (
                                   select count(*)
                                   from dining_tables dt
                                   where dt.status = 'available'
                                     and dt.capacity >= we.party_size
                               ) as available_table_count,
                               (
                                   select dt.code
                                   from dining_tables dt
                                   where dt.status = 'available'
                                     and dt.capacity >= we.party_size
                                   order by dt.capacity, dt.code
                                   limit 1
                               ) as suggested_table_code
                        from waitlist_entries we
                        where we.status in ('waiting', 'notified', 'skipped', 'expired')
                        order by case we.status when 'notified' then 0 when 'waiting' then 1 when 'skipped' then 2 else 3 end,
                                 we.priority desc,
                                 we.added_at
                        """)
                .query((rs, rowNum) -> new ReservationViews.WaitlistView(
                        rs.getObject("waitlist_entry_id", UUID.class),
                        rs.getString("contact_name"),
                        rs.getString("contact_phone"),
                        rs.getString("contact_email"),
                        rs.getInt("party_size"),
                        "~" + rs.getInt("quoted_wait_min") + " min",
                        rs.getString("status"),
                        rs.getTimestamp("hold_expires_at") == null ? null : rs.getTimestamp("hold_expires_at").toInstant().toString(),
                        rs.getString("notes"),
                        rs.getInt("available_table_count"),
                        rs.getString("suggested_table_code") == null
                                ? null
                                : SA.irms.common.support.TableCodeParser.parseTableNumber(rs.getString("suggested_table_code"))
                ))
                .list();

        return new ReservationViews.ReservationOverview(tables, reservations, waitlist);
    }

    public ReservationViews.ReservationView findReservation(UUID reservationId) {
        return jdbcClient.sql("""
                        select r.reservation_id,
                               c.name,
                               c.phone,
                               c.email,
                               r.party_size,
                               r.arrival_at,
                               r.status,
                               r.notes,
                               dt.code as table_code
                        from reservations r
                        join reservation_contacts c on c.contact_id = r.contact_id
                        left join table_assignments ta on ta.reservation_id = r.reservation_id and ta.released_at is null
                        left join dining_tables dt on dt.table_id = ta.table_id
                        where r.reservation_id = :reservationId
                        """)
                .param("reservationId", reservationId)
                .query((rs, rowNum) -> new ReservationViews.ReservationView(
                        rs.getObject("reservation_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getInt("party_size"),
                        rs.getTimestamp("arrival_at").toInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString(),
                        ReservationTimeSupport.formatTime(rs.getTimestamp("arrival_at").toInstant()),
                        ReservationTimeSupport.mapReservationStatus(rs.getString("status")),
                        rs.getString("table_code") == null ? null : SA.irms.common.support.TableCodeParser.parseTableNumber(rs.getString("table_code")),
                        rs.getString("notes")
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Reservation was not found."));
    }

    public ReservationRow loadReservationRow(UUID reservationId) {
        return jdbcClient.sql("""
                        select r.reservation_id,
                               c.name,
                               c.phone,
                               c.email,
                               r.party_size,
                               r.arrival_at,
                               r.status,
                               r.notes
                        from reservations r
                        join reservation_contacts c on c.contact_id = r.contact_id
                        where r.reservation_id = :reservationId
                        """)
                .param("reservationId", reservationId)
                .query((rs, rowNum) -> new ReservationRow(
                        rs.getObject("reservation_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getInt("party_size"),
                        rs.getTimestamp("arrival_at").toInstant(),
                        rs.getString("status"),
                        rs.getString("notes")
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Reservation was not found."));
    }
}
