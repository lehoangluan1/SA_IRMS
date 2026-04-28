package SA.irms.reservation.infrastructure.persistence;
import SA.irms.reservation.application.query.ActiveTableState;

import SA.irms.reservation.application.port.out.TableQueryRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.common.error.NotFoundException;

@Repository
public class JdbcTableQueryRepository implements TableQueryRepository {
    private final JdbcClient jdbcClient;

    JdbcTableQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public SA.irms.reservation.application.view.ReservationViews.TableView findTable(UUID tableId) {
        return jdbcClient.sql("""
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
                        where dt.table_id = :tableId
                        """)
                .param("tableId", tableId)
                .query((rs, rowNum) -> new SA.irms.reservation.application.view.ReservationViews.TableView(
                        rs.getObject("table_id", UUID.class),
                        SA.irms.common.support.TableCodeParser.parseTableNumber(rs.getString("code")),
                        rs.getInt("capacity"),
                        rs.getString("status"),
                        rs.getString("zone") + (rs.getString("section_label") == null ? "" : " - " + rs.getString("section_label")),
                        rs.getObject("session_id", UUID.class),
                        rs.getTimestamp("opened_at") == null ? null : formatTime(rs.getTimestamp("opened_at").toInstant()),
                        rs.getTimestamp("arrival_at") == null ? null : formatTime(rs.getTimestamp("arrival_at").toInstant())
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Table was not found."));
    }

    public ActiveTableState loadActiveTableState(UUID tableId) {
        return jdbcClient.sql("""
                        select ts.session_id,
                               exists (
                                   select 1
                                   from bills b
                                   where b.table_session_id = ts.session_id
                                     and b.status in ('open', 'finalized', 'partially_paid')
                               ) as has_outstanding_balance
                        from table_sessions ts
                        where ts.table_id = :tableId
                          and ts.status in ('active', 'billing')
                        order by ts.opened_at desc
                        limit 1
                        """)
                .param("tableId", tableId)
                .query((rs, rowNum) -> new ActiveTableState(rs.getObject("session_id", UUID.class), rs.getBoolean("has_outstanding_balance")))
                .optional()
                .orElse(new ActiveTableState(null, false));
    }

    public String findSuggestedWaitlistGuest(int tableCapacity) {
        return jdbcClient.sql("""
                        select contact_name
                        from waitlist_entries
                        where status in ('waiting', 'notified')
                          and party_size <= :tableCapacity
                        order by case when status = 'notified' then 0 else 1 end,
                                 priority desc,
                                 added_at
                        limit 1
                        """)
                .param("tableCapacity", tableCapacity)
                .query(String.class)
                .optional()
                .orElse(null);
    }

    private String formatTime(Instant instant) {
        return DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneOffset.UTC).format(instant);
    }
}