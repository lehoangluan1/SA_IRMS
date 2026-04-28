package SA.irms.reporting.infrastructure.persistence;

import SA.irms.reporting.application.port.out.OperationsDashboardQueryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOperationsDashboardQueryRepository implements OperationsDashboardQueryRepository {
    private final JdbcClient jdbcClient;

    public JdbcOperationsDashboardQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<AlertRow> loadOpenLowStockAlerts() {
        return jdbcClient.sql("""
                        select alert_id::text as id,
                               severity,
                               created_at
                        from low_stock_alerts
                        where status = 'open'
                        order by created_at desc
                        limit 4
                        """)
                .query((rs, rowNum) -> new AlertRow(
                        rs.getString("id"),
                        rs.getString("severity"),
                        rs.getTimestamp("created_at").toInstant()))
                .list();
    }

    @Override
    public List<AuditAlertRow> loadAuditAlerts() {
        return jdbcClient.sql("""
                        select audit_log_id::text as id,
                               action,
                               reason,
                               recorded_at
                        from audit_logs
                        where action in ('billing.refund.issued', 'inventory.manual_adjustment', 'billing.bill.reopened')
                        order by recorded_at desc
                        limit 2
                        """)
                .query((rs, rowNum) -> new AuditAlertRow(
                        rs.getString("id"),
                        rs.getString("action"),
                        rs.getString("reason"),
                        rs.getTimestamp("recorded_at").toInstant()))
                .list();
    }

    @Override
    public List<ActiveOrderRow> loadActiveOrders() {
        return jdbcClient.sql("""
                        select o.order_id::text as order_id,
                               o.server_user_id,
                               dt.code as table_code,
                               o.status,
                               o.created_at,
                               count(oi.order_item_id) as item_count
                        from orders o
                        join table_sessions ts on ts.session_id = o.table_session_id
                        join dining_tables dt on dt.table_id = ts.table_id
                        left join order_items oi on oi.order_id = o.order_id
                        where ts.status in ('active', 'billing')
                          and o.status in ('confirmed', 'in_progress', 'ready')
                        group by o.order_id, o.server_user_id, dt.code, o.status, o.created_at
                        order by o.created_at desc
                        limit 5
                        """)
                .query((rs, rowNum) -> new ActiveOrderRow(
                        rs.getString("order_id"),
                        rs.getObject("server_user_id", UUID.class),
                        SA.irms.common.support.TableCodeParser.parseTableNumber(rs.getString("table_code")),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getInt("item_count")
                ))
                .list();
    }

    @Override
    public TodayFinancials loadTodayFinancials() {
        return jdbcClient.sql("""
                        with paid_today as (
                            select amount
                            from payments
                            where status = 'completed'
                              and paid_at >= date_trunc('day', now())
                              and paid_at < date_trunc('day', now()) + interval '1 day'
                        ),
                        refunds_today as (
                            select amount
                            from refunds
                            where status = 'completed'
                              and processed_at >= date_trunc('day', now())
                              and processed_at < date_trunc('day', now()) + interval '1 day'
                        )
                        select coalesce((select sum(amount) from paid_today), 0) as revenue,
                               coalesce((select count(*) from paid_today), 0) as transactions,
                               coalesce((select sum(amount) from refunds_today), 0) as refunds
                        """)
                .query((rs, rowNum) -> new TodayFinancials(
                        rs.getBigDecimal("revenue"),
                        rs.getLong("transactions"),
                        rs.getBigDecimal("refunds")
                ))
                .single();
    }

    @Override
    public List<RevenueTrendRow> loadTodayRevenueTrend() {
        return jdbcClient.sql("""
                        select to_char(date_trunc('hour', paid_at), 'HH24:00') as hour,
                               coalesce(sum(amount), 0) as revenue
                        from payments
                        where status = 'completed'
                          and paid_at >= date_trunc('day', now())
                          and paid_at < date_trunc('day', now()) + interval '1 day'
                        group by date_trunc('hour', paid_at)
                        order by date_trunc('hour', paid_at)
                        """)
                .query((rs, rowNum) -> new RevenueTrendRow(
                        rs.getString("hour"),
                        rs.getBigDecimal("revenue")
                ))
                .list();
    }

    @Override
    public String loadInventoryName(String alertId) {
        return jdbcClient.sql("""
                        select i.name || ' (' || i.on_hand || ' ' || i.unit || ' remaining)'
                        from low_stock_alerts a
                        join inventory_items i on i.inventory_item_id = a.inventory_item_id
                        where a.alert_id::text = :alertId
                        """)
                .param("alertId", alertId)
                .query(String.class)
                .single();
    }
}
