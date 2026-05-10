package SA.irms.reservation.infrastructure.persistence;
import SA.irms.reservation.application.query.WaitlistRow;

import SA.irms.reservation.application.port.out.WaitlistQueryRepository;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.common.error.ConflictException;
import SA.irms.common.error.NotFoundException;

@Repository
public class JdbcWaitlistQueryRepository implements WaitlistQueryRepository {
    private final JdbcClient jdbcClient;

    JdbcWaitlistQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public SA.irms.reservation.application.view.ReservationViews.WaitlistView findWaitlistEntry(UUID waitlistEntryId) {
        return jdbcClient.sql("""
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
                        where we.waitlist_entry_id = :waitlistEntryId
                        """)
                .param("waitlistEntryId", waitlistEntryId)
                .query((rs, rowNum) -> new SA.irms.reservation.application.view.ReservationViews.WaitlistView(
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
                        rs.getString("suggested_table_code") == null ? null : SA.irms.common.support.TableCodeParser.parseTableNumber(rs.getString("suggested_table_code"))
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Waitlist entry was not found."));
    }

    public WaitlistRow loadWaitlistRow(UUID waitlistEntryId) {
        return jdbcClient.sql("""
                        select waitlist_entry_id, party_size, status, priority, hold_expires_at
                        from waitlist_entries
                        where waitlist_entry_id = :waitlistEntryId
                        """)
                .param("waitlistEntryId", waitlistEntryId)
                .query((rs, rowNum) -> new WaitlistRow(
                        rs.getObject("waitlist_entry_id", UUID.class),
                        rs.getInt("party_size"),
                        rs.getString("status"),
                        rs.getInt("priority"),
                        rs.getTimestamp("hold_expires_at") == null ? null : rs.getTimestamp("hold_expires_at").toInstant()
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Waitlist entry was not found."));
    }

    public int estimateQuotedWaitMinutes() {
        int queueDepth = jdbcClient.sql("""
                        select count(*)
                        from waitlist_entries
                        where status in ('waiting', 'notified')
                        """)
                .query(Integer.class)
                .single();
        return 15 + (queueDepth * 10);
    }

    public UUID requireAvailableTableIdForWaitlist(int partySize) {
        return jdbcClient.sql("""
                        select table_id
                        from dining_tables
                        where status = 'available'
                          and capacity >= :partySize
                        order by capacity, code
                        limit 1
                        """)
                .param("partySize", partySize)
                .query(UUID.class)
                .optional()
                .orElseThrow(() -> new ConflictException("No suitable table is available for this waitlist party."));
    }
}