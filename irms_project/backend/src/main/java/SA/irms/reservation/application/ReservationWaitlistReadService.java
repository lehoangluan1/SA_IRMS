package SA.irms.reservation.application;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import SA.irms.common.error.ConflictException;
import SA.irms.common.error.NotFoundException;
import SA.irms.reservation.application.view.ReservationViews;

@Service
class ReservationWaitlistReadService {
    private final JdbcClient jdbcClient;

    ReservationWaitlistReadService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    ReservationViews.WaitlistView findWaitlistEntry(UUID waitlistEntryId) {
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
                        rs.getString("suggested_table_code") == null ? null : SA.irms.common.support.TableCodeParser.parseTableNumber(rs.getString("suggested_table_code"))
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Waitlist entry was not found."));
    }

    WaitlistRow loadWaitlistRow(UUID waitlistEntryId) {
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

    int estimateQuotedWaitMinutes() {
        int queueDepth = jdbcClient.sql("""
                        select count(*)
                        from waitlist_entries
                        where status in ('waiting', 'notified')
                        """)
                .query(Integer.class)
                .single();
        return 15 + (queueDepth * 10);
    }

    UUID requireAvailableTableIdForWaitlist(int partySize) {
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
