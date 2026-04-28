package SA.irms.reporting.application.view;

import java.time.Instant;
import java.util.List;

public final class TypedReportViews {
    private TypedReportViews() {
    }

    private static final List<ReportColumn> SALES_COLUMNS = List.of(
            new ReportColumn("business_date", "Business Date", ReportValueType.DATE, 0),
            new ReportColumn("order_count", "Order Count", ReportValueType.INTEGER, 1),
            new ReportColumn("bill_count", "Bill Count", ReportValueType.INTEGER, 2),
            new ReportColumn("gross_sales", "Gross Sales", ReportValueType.MONEY, 3),
            new ReportColumn("refund_total", "Refund Total", ReportValueType.MONEY, 4),
            new ReportColumn("net_sales", "Net Sales", ReportValueType.MONEY, 5)
    );

    private static final List<ReportColumn> PEAK_HOUR_COLUMNS = List.of(
            new ReportColumn("business_date", "Business Date", ReportValueType.DATE, 0),
            new ReportColumn("hour_of_day", "Hour Of Day", ReportValueType.INTEGER, 1),
            new ReportColumn("order_count", "Order Count", ReportValueType.INTEGER, 2),
            new ReportColumn("revenue_total", "Revenue Total", ReportValueType.MONEY, 3)
    );

    private static final List<ReportColumn> BEST_SELLING_ITEM_COLUMNS = List.of(
            new ReportColumn("business_date", "Business Date", ReportValueType.DATE, 0),
            new ReportColumn("menu_item_id", "Menu Item Id", ReportValueType.TEXT, 1),
            new ReportColumn("item_name", "Item Name", ReportValueType.TEXT, 2),
            new ReportColumn("quantity_sold", "Quantity Sold", ReportValueType.INTEGER, 3),
            new ReportColumn("revenue_total", "Revenue Total", ReportValueType.MONEY, 4)
    );

    private static final List<ReportColumn> REVENUE_COLUMNS = List.of(
            new ReportColumn("business_date", "Business Date", ReportValueType.DATE, 0),
            new ReportColumn("payment_method", "Payment Method", ReportValueType.TEXT, 1),
            new ReportColumn("gross_revenue", "Gross Revenue", ReportValueType.MONEY, 2),
            new ReportColumn("discounts", "Discounts", ReportValueType.MONEY, 3),
            new ReportColumn("refunds", "Refunds", ReportValueType.MONEY, 4),
            new ReportColumn("net_revenue", "Net Revenue", ReportValueType.MONEY, 5)
    );

    private static final List<ReportColumn> KITCHEN_BOTTLENECK_COLUMNS = List.of(
            new ReportColumn("business_date", "Business Date", ReportValueType.DATE, 0),
            new ReportColumn("station", "Station", ReportValueType.TEXT, 1),
            new ReportColumn("delayed_item_count", "Delayed Item Count", ReportValueType.INTEGER, 2),
            new ReportColumn("average_delay_minutes", "Average Delay Minutes", ReportValueType.DURATION_MINUTES, 3)
    );

    private static final List<ReportColumn> STAFF_EFFICIENCY_COLUMNS = List.of(
            new ReportColumn("business_date", "Business Date", ReportValueType.DATE, 0),
            new ReportColumn("staff_id", "Staff Id", ReportValueType.TEXT, 1),
            new ReportColumn("role", "Role", ReportValueType.TEXT, 2),
            new ReportColumn("completed_tasks", "Completed Tasks", ReportValueType.INTEGER, 3),
            new ReportColumn("average_service_time", "Average Service Time", ReportValueType.DURATION_MINUTES, 4)
    );

    private static final List<ReportColumn> INVENTORY_USAGE_COLUMNS = List.of(
            new ReportColumn("business_date", "Business Date", ReportValueType.DATE, 0),
            new ReportColumn("ingredient_id", "Ingredient Id", ReportValueType.TEXT, 1),
            new ReportColumn("ingredient_name", "Ingredient Name", ReportValueType.TEXT, 2),
            new ReportColumn("quantity_used", "Quantity Used", ReportValueType.DECIMAL, 3),
            new ReportColumn("waste_quantity", "Waste Quantity", ReportValueType.DECIMAL, 4)
    );

    private static final List<ReportColumn> COMBO_SALES_COLUMNS = List.of(
            new ReportColumn("business_date", "Business Date", ReportValueType.DATE, 0),
            new ReportColumn("combo_id", "Combo Id", ReportValueType.TEXT, 1),
            new ReportColumn("combo_name", "Combo Name", ReportValueType.TEXT, 2),
            new ReportColumn("quantity_sold", "Quantity Sold", ReportValueType.INTEGER, 3),
            new ReportColumn("revenue_total", "Revenue Total", ReportValueType.MONEY, 4)
    );

    public record SalesReportView(
            Instant generatedAt,
            Instant periodStart,
            Instant periodEnd,
            List<ReportingProjectionRows.SalesReportRow> rows,
            List<ReportMetricView> metrics
    ) implements TabularReportView<ReportingProjectionRows.SalesReportRow> {
        public SalesReportView {
            rows = List.copyOf(rows == null ? List.of() : rows);
            metrics = List.copyOf(metrics == null ? List.of() : metrics);
        }

        @Override
        public String type() { return "sales"; }
        @Override
        public List<ReportColumn> columns() { return SALES_COLUMNS; }
    }

    public record PeakHourReportView(
            Instant generatedAt,
            Instant periodStart,
            Instant periodEnd,
            List<ReportingProjectionRows.PeakHourReportRow> rows,
            List<ReportMetricView> metrics
    ) implements TabularReportView<ReportingProjectionRows.PeakHourReportRow> {
        public PeakHourReportView {
            rows = List.copyOf(rows == null ? List.of() : rows);
            metrics = List.copyOf(metrics == null ? List.of() : metrics);
        }

        @Override
        public String type() { return "peak-hours"; }
        @Override
        public List<ReportColumn> columns() { return PEAK_HOUR_COLUMNS; }
    }

    public record BestSellingItemReportView(
            Instant generatedAt,
            Instant periodStart,
            Instant periodEnd,
            List<ReportingProjectionRows.BestSellingItemReportRow> rows,
            List<ReportMetricView> metrics
    ) implements TabularReportView<ReportingProjectionRows.BestSellingItemReportRow> {
        public BestSellingItemReportView {
            rows = List.copyOf(rows == null ? List.of() : rows);
            metrics = List.copyOf(metrics == null ? List.of() : metrics);
        }

        @Override
        public String type() { return "best-selling-items"; }
        @Override
        public List<ReportColumn> columns() { return BEST_SELLING_ITEM_COLUMNS; }
    }

    public record RevenueReportView(
            Instant generatedAt,
            Instant periodStart,
            Instant periodEnd,
            List<ReportingProjectionRows.RevenueReportRow> rows,
            List<ReportMetricView> metrics
    ) implements TabularReportView<ReportingProjectionRows.RevenueReportRow> {
        public RevenueReportView {
            rows = List.copyOf(rows == null ? List.of() : rows);
            metrics = List.copyOf(metrics == null ? List.of() : metrics);
        }

        @Override
        public String type() { return "revenue"; }
        @Override
        public List<ReportColumn> columns() { return REVENUE_COLUMNS; }
    }

    public record KitchenBottleneckReportView(
            Instant generatedAt,
            Instant periodStart,
            Instant periodEnd,
            List<ReportingProjectionRows.KitchenBottleneckReportRow> rows,
            List<ReportMetricView> metrics
    ) implements TabularReportView<ReportingProjectionRows.KitchenBottleneckReportRow> {
        public KitchenBottleneckReportView {
            rows = List.copyOf(rows == null ? List.of() : rows);
            metrics = List.copyOf(metrics == null ? List.of() : metrics);
        }

        @Override
        public String type() { return "kitchen-bottlenecks"; }
        @Override
        public List<ReportColumn> columns() { return KITCHEN_BOTTLENECK_COLUMNS; }
    }

    public record StaffEfficiencyReportView(
            Instant generatedAt,
            Instant periodStart,
            Instant periodEnd,
            List<ReportingProjectionRows.StaffEfficiencyReportRow> rows,
            List<ReportMetricView> metrics
    ) implements TabularReportView<ReportingProjectionRows.StaffEfficiencyReportRow> {
        public StaffEfficiencyReportView {
            rows = List.copyOf(rows == null ? List.of() : rows);
            metrics = List.copyOf(metrics == null ? List.of() : metrics);
        }

        @Override
        public String type() { return "staff-efficiency"; }
        @Override
        public List<ReportColumn> columns() { return STAFF_EFFICIENCY_COLUMNS; }
    }

    public record InventoryUsageReportView(
            Instant generatedAt,
            Instant periodStart,
            Instant periodEnd,
            List<ReportingProjectionRows.InventoryUsageReportRow> rows,
            List<ReportMetricView> metrics
    ) implements TabularReportView<ReportingProjectionRows.InventoryUsageReportRow> {
        public InventoryUsageReportView {
            rows = List.copyOf(rows == null ? List.of() : rows);
            metrics = List.copyOf(metrics == null ? List.of() : metrics);
        }

        @Override
        public String type() { return "inventory-usage"; }
        @Override
        public List<ReportColumn> columns() { return INVENTORY_USAGE_COLUMNS; }
    }

    public record ComboSalesReportView(
            Instant generatedAt,
            Instant periodStart,
            Instant periodEnd,
            List<ReportingProjectionRows.ComboSalesReportRow> rows,
            List<ReportMetricView> metrics
    ) implements TabularReportView<ReportingProjectionRows.ComboSalesReportRow> {
        public ComboSalesReportView {
            rows = List.copyOf(rows == null ? List.of() : rows);
            metrics = List.copyOf(metrics == null ? List.of() : metrics);
        }

        @Override
        public String type() { return "combo-sales"; }
        @Override
        public List<ReportColumn> columns() { return COMBO_SALES_COLUMNS; }
    }
}
