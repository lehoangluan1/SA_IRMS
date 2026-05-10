package SA.irms.reporting.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.reporting.application.port.out.ReportingProjectionRepository;
import SA.irms.reporting.application.view.ReportingProjectionRows;

final class JdbcReportProjectionQueryRepository extends ReportingProjectionJdbcSupport {
    JdbcReportProjectionQueryRepository(JdbcClient jdbcClient) {
        super(jdbcClient);
    }

    ReportingProjectionRepository.OperationsMetrics loadOperationsMetrics() {
        return new ReportingProjectionRepository.OperationsMetrics(
                countActiveTables(),
                countOpenOrders(),
                countReadyOrders(),
                countKitchenQueue(),
                averageKitchenWaitMinutes(),
                countOpenLowStockAlerts()
        );
    }

    List<ReportingProjectionRows.SalesReportRow> findSalesRows() {
        return jdbcClient.sql("""
                        select business_date, order_count, bill_count, gross_sales, refund_total, net_sales
                        from reporting_sales_projection
                        order by business_date desc
                        limit 30
                        """)
                .query((rs, rowNum) -> new ReportingProjectionRows.SalesReportRow(
                        rs.getObject("business_date", LocalDate.class),
                        rs.getLong("order_count"),
                        rs.getLong("bill_count"),
                        rs.getBigDecimal("gross_sales"),
                        rs.getBigDecimal("refund_total"),
                        rs.getBigDecimal("net_sales")
                ))
                .list();
    }

    List<ReportingProjectionRows.PeakHourReportRow> findPeakHourRows() {
        return jdbcClient.sql("""
                        select business_date, hour_of_day, order_count, revenue_total
                        from reporting_peak_hour_projection
                        order by business_date desc, hour_of_day desc
                        limit 48
                        """)
                .query((rs, rowNum) -> new ReportingProjectionRows.PeakHourReportRow(
                        rs.getObject("business_date", LocalDate.class),
                        rs.getInt("hour_of_day"),
                        rs.getLong("order_count"),
                        rs.getBigDecimal("revenue_total")
                ))
                .list();
    }

    List<ReportingProjectionRows.BestSellingItemReportRow> findBestSellingItemRows() {
        return jdbcClient.sql("""
                        select business_date, menu_item_id, item_name, quantity_sold, revenue_total
                        from reporting_best_selling_item_projection
                        order by business_date desc, quantity_sold desc
                        limit 50
                        """)
                .query((rs, rowNum) -> new ReportingProjectionRows.BestSellingItemReportRow(
                        rs.getObject("business_date", LocalDate.class),
                        rs.getString("menu_item_id"),
                        rs.getString("item_name"),
                        rs.getLong("quantity_sold"),
                        rs.getBigDecimal("revenue_total")
                ))
                .list();
    }

    List<ReportingProjectionRows.RevenueReportRow> findRevenueRows() {
        return jdbcClient.sql("""
                        select business_date, payment_method, gross_revenue, discounts, refunds, net_revenue
                        from reporting_revenue_projection
                        order by business_date desc, payment_method
                        """)
                .query((rs, rowNum) -> new ReportingProjectionRows.RevenueReportRow(
                        rs.getObject("business_date", LocalDate.class),
                        rs.getString("payment_method"),
                        rs.getBigDecimal("gross_revenue"),
                        rs.getBigDecimal("discounts"),
                        rs.getBigDecimal("refunds"),
                        rs.getBigDecimal("net_revenue")
                ))
                .list();
    }

    List<ReportingProjectionRows.KitchenBottleneckReportRow> findKitchenBottleneckRows() {
        return jdbcClient.sql("""
                        select business_date, station, delayed_item_count, average_delay_minutes
                        from reporting_kitchen_bottleneck_projection
                        order by business_date desc, delayed_item_count desc
                        limit 50
                        """)
                .query((rs, rowNum) -> new ReportingProjectionRows.KitchenBottleneckReportRow(
                        rs.getObject("business_date", LocalDate.class),
                        rs.getString("station"),
                        rs.getLong("delayed_item_count"),
                        rs.getBigDecimal("average_delay_minutes")
                ))
                .list();
    }

    List<ReportingProjectionRows.StaffEfficiencyReportRow> findStaffEfficiencyRows() {
        return jdbcClient.sql("""
                        select business_date, staff_id, role, completed_tasks, average_service_time
                        from reporting_staff_efficiency_projection
                        order by business_date desc, completed_tasks desc
                        limit 50
                        """)
                .query((rs, rowNum) -> new ReportingProjectionRows.StaffEfficiencyReportRow(
                        rs.getObject("business_date", LocalDate.class),
                        rs.getString("staff_id"),
                        rs.getString("role"),
                        rs.getLong("completed_tasks"),
                        rs.getBigDecimal("average_service_time")
                ))
                .list();
    }

    List<ReportingProjectionRows.InventoryUsageReportRow> findInventoryUsageRows() {
        return jdbcClient.sql("""
                        select business_date, ingredient_id, ingredient_name, quantity_used, waste_quantity
                        from reporting_inventory_usage_projection
                        order by business_date desc, quantity_used desc
                        limit 50
                        """)
                .query((rs, rowNum) -> new ReportingProjectionRows.InventoryUsageReportRow(
                        rs.getObject("business_date", LocalDate.class),
                        rs.getString("ingredient_id"),
                        rs.getString("ingredient_name"),
                        rs.getBigDecimal("quantity_used"),
                        rs.getBigDecimal("waste_quantity")
                ))
                .list();
    }

    List<ReportingProjectionRows.ComboSalesReportRow> findComboSalesRows() {
        return jdbcClient.sql("""
                        select business_date, combo_id, combo_name, quantity_sold, revenue_total
                        from reporting_combo_sales_projection
                        order by business_date desc, quantity_sold desc
                        limit 50
                        """)
                .query((rs, rowNum) -> new ReportingProjectionRows.ComboSalesReportRow(
                        rs.getObject("business_date", LocalDate.class),
                        rs.getString("combo_id"),
                        rs.getString("combo_name"),
                        rs.getLong("quantity_sold"),
                        rs.getBigDecimal("revenue_total")
                ))
                .list();
    }

    private long countActiveTables() { return jdbcClient.sql("select count(*) from table_sessions where status in ('active', 'billing')").query(Long.class).single(); }
    private long countOpenOrders() { return jdbcClient.sql("select count(*) from orders o join table_sessions ts on ts.session_id = o.table_session_id where ts.status in ('active', 'billing') and o.status in ('confirmed', 'in_progress', 'ready')").query(Long.class).single(); }
    private long countReadyOrders() { return jdbcClient.sql("select count(*) from orders o join table_sessions ts on ts.session_id = o.table_session_id where ts.status in ('active', 'billing') and o.status = 'ready'").query(Long.class).single(); }
    private long countKitchenQueue() { return jdbcClient.sql("select count(*) from kitchen_ticket_items kti join kitchen_tickets kt on kt.ticket_id = kti.ticket_id where kt.status in ('queued', 'cooking', 'ready') and kti.status in ('queued', 'cooking', 'ready')").query(Long.class).single(); }
    private long averageKitchenWaitMinutes() { return jdbcClient.sql("select coalesce(ceil(avg(extract(epoch from (now() - kt.created_at)) / 60.0)), 0)::bigint from kitchen_tickets kt where kt.status in ('queued', 'cooking', 'ready')").query(Long.class).single(); }
    private long countOpenLowStockAlerts() { return jdbcClient.sql("select count(*) from low_stock_alerts where status = 'open'").query(Long.class).single(); }
}
