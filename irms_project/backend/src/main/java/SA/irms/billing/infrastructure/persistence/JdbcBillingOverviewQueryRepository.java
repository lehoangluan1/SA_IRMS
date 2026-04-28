package SA.irms.billing.infrastructure.persistence;

import SA.irms.billing.application.port.out.BillingOverviewQueryRepository;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBillingOverviewQueryRepository implements BillingOverviewQueryRepository {
    private final JdbcClient jdbcClient;

    JdbcBillingOverviewQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<BillableSessionSummary> loadBillableSessions() {
        return jdbcClient.sql("""
                        select ts.session_id,
                               dt.code as table_code,
                               ts.guest_count,
                               ts.opened_at
                        from table_sessions ts
                        join dining_tables dt on dt.table_id = ts.table_id
                        where ts.status in ('active', 'billing')
                          and exists (
                              select 1
                              from orders o
                              where o.table_session_id = ts.session_id
                          )
                          and not exists (
                              select 1
                              from bills b
                              where b.table_session_id = ts.session_id
                          )
                        order by ts.opened_at desc
                        """)
                .query((rs, rowNum) -> new BillableSessionSummary(
                        rs.getObject("session_id", UUID.class),
                        SA.irms.common.support.TableCodeParser.parseTableNumber(rs.getString("table_code")),
                        rs.getInt("guest_count"),
                        rs.getTimestamp("opened_at").toInstant()
                ))
                .list();
    }

    @Override
    public List<RecentBillSummary> loadRecentBills() {
        return jdbcClient.sql("""
                        select b.bill_id,
                               dt.code as table_code,
                               b.grand_total,
                               b.status,
                               coalesce(max(p.method), 'pending') as payment_method,
                               max(coalesce(p.paid_at, b.created_at)) as activity_at
                        from bills b
                        join table_sessions ts on ts.session_id = b.table_session_id
                        join dining_tables dt on dt.table_id = ts.table_id
                        left join payments p on p.bill_id = b.bill_id
                        group by b.bill_id, dt.code, b.grand_total, b.status
                        order by activity_at desc
                        limit 8
                        """)
                .query((rs, rowNum) -> new RecentBillSummary(
                        rs.getObject("bill_id", UUID.class),
                        SA.irms.common.support.TableCodeParser.parseTableNumber(rs.getString("table_code")),
                        rs.getBigDecimal("grand_total"),
                        rs.getString("status"),
                        rs.getString("payment_method"),
                        rs.getTimestamp("activity_at").toInstant()
                ))
                .list();
    }
}
