package SA.irms.reporting.application.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ReportingProjectionRows {
    private ReportingProjectionRows() {
    }

    public record SalesReportRow(
            LocalDate businessDate,
            long orderCount,
            long billCount,
            BigDecimal grossSales,
            BigDecimal refundTotal,
            BigDecimal netSales
    ) implements TabularReportRow {
        @Override
        public List<ReportCellView> cells() {
            return List.of(
                    cell("business_date", ReportValueType.DATE, businessDate),
                    cell("order_count", ReportValueType.INTEGER, orderCount),
                    cell("bill_count", ReportValueType.INTEGER, billCount),
                    cell("gross_sales", ReportValueType.MONEY, grossSales),
                    cell("refund_total", ReportValueType.MONEY, refundTotal),
                    cell("net_sales", ReportValueType.MONEY, netSales)
            );
        }
    }

    public record PeakHourReportRow(
            LocalDate businessDate,
            int hourOfDay,
            long orderCount,
            BigDecimal revenueTotal
    ) implements TabularReportRow {
        @Override
        public List<ReportCellView> cells() {
            return List.of(
                    cell("business_date", ReportValueType.DATE, businessDate),
                    cell("hour_of_day", ReportValueType.INTEGER, hourOfDay),
                    cell("order_count", ReportValueType.INTEGER, orderCount),
                    cell("revenue_total", ReportValueType.MONEY, revenueTotal)
            );
        }
    }

    public record BestSellingItemReportRow(
            LocalDate businessDate,
            String menuItemId,
            String itemName,
            long quantitySold,
            BigDecimal revenueTotal
    ) implements TabularReportRow {
        @Override
        public List<ReportCellView> cells() {
            return List.of(
                    cell("business_date", ReportValueType.DATE, businessDate),
                    cell("menu_item_id", ReportValueType.TEXT, menuItemId),
                    cell("item_name", ReportValueType.TEXT, itemName),
                    cell("quantity_sold", ReportValueType.INTEGER, quantitySold),
                    cell("revenue_total", ReportValueType.MONEY, revenueTotal)
            );
        }
    }

    public record RevenueReportRow(
            LocalDate businessDate,
            String paymentMethod,
            BigDecimal grossRevenue,
            BigDecimal discounts,
            BigDecimal refunds,
            BigDecimal netRevenue
    ) implements TabularReportRow {
        @Override
        public List<ReportCellView> cells() {
            return List.of(
                    cell("business_date", ReportValueType.DATE, businessDate),
                    cell("payment_method", ReportValueType.TEXT, paymentMethod),
                    cell("gross_revenue", ReportValueType.MONEY, grossRevenue),
                    cell("discounts", ReportValueType.MONEY, discounts),
                    cell("refunds", ReportValueType.MONEY, refunds),
                    cell("net_revenue", ReportValueType.MONEY, netRevenue)
            );
        }
    }

    public record KitchenBottleneckReportRow(
            LocalDate businessDate,
            String station,
            long delayedItemCount,
            BigDecimal averageDelayMinutes
    ) implements TabularReportRow {
        @Override
        public List<ReportCellView> cells() {
            return List.of(
                    cell("business_date", ReportValueType.DATE, businessDate),
                    cell("station", ReportValueType.TEXT, station),
                    cell("delayed_item_count", ReportValueType.INTEGER, delayedItemCount),
                    cell("average_delay_minutes", ReportValueType.DURATION_MINUTES, averageDelayMinutes)
            );
        }
    }

    public record StaffEfficiencyReportRow(
            LocalDate businessDate,
            String staffId,
            String role,
            long completedTasks,
            BigDecimal averageServiceTime
    ) implements TabularReportRow {
        @Override
        public List<ReportCellView> cells() {
            return List.of(
                    cell("business_date", ReportValueType.DATE, businessDate),
                    cell("staff_id", ReportValueType.TEXT, staffId),
                    cell("role", ReportValueType.TEXT, role),
                    cell("completed_tasks", ReportValueType.INTEGER, completedTasks),
                    cell("average_service_time", ReportValueType.DURATION_MINUTES, averageServiceTime)
            );
        }
    }

    public record InventoryUsageReportRow(
            LocalDate businessDate,
            String ingredientId,
            String ingredientName,
            BigDecimal quantityUsed,
            BigDecimal wasteQuantity
    ) implements TabularReportRow {
        @Override
        public List<ReportCellView> cells() {
            return List.of(
                    cell("business_date", ReportValueType.DATE, businessDate),
                    cell("ingredient_id", ReportValueType.TEXT, ingredientId),
                    cell("ingredient_name", ReportValueType.TEXT, ingredientName),
                    cell("quantity_used", ReportValueType.DECIMAL, quantityUsed),
                    cell("waste_quantity", ReportValueType.DECIMAL, wasteQuantity)
            );
        }
    }

    public record ComboSalesReportRow(
            LocalDate businessDate,
            String comboId,
            String comboName,
            long quantitySold,
            BigDecimal revenueTotal
    ) implements TabularReportRow {
        @Override
        public List<ReportCellView> cells() {
            return List.of(
                    cell("business_date", ReportValueType.DATE, businessDate),
                    cell("combo_id", ReportValueType.TEXT, comboId),
                    cell("combo_name", ReportValueType.TEXT, comboName),
                    cell("quantity_sold", ReportValueType.INTEGER, quantitySold),
                    cell("revenue_total", ReportValueType.MONEY, revenueTotal)
            );
        }
    }

    private static ReportCellView cell(String columnKey, ReportValueType valueType, Object value) {
        return new ReportCellView(columnKey, display(value), valueType);
    }

    private static String display(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }
}
