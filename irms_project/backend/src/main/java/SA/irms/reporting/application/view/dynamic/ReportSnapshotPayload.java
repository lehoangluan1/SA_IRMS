package SA.irms.reporting.application.view.dynamic;

import SA.irms.reporting.application.view.OperationsReportView;
import SA.irms.reporting.application.view.ReportValueType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public record ReportSnapshotPayload(
        String sourceEventId,
        DynamicReportView reportView
) {
    private static final String METRIC_KEY_COLUMN = "metricKey";
    private static final String METRIC_LABEL_COLUMN = "metricLabel";
    private static final String METRIC_VALUE_COLUMN = "metricValue";

    private static final List<DynamicReportColumn> OPERATIONS_COLUMNS = List.of(
            new DynamicReportColumn(METRIC_KEY_COLUMN, "Metric Key", ReportValueType.TEXT, 0),
            new DynamicReportColumn(METRIC_LABEL_COLUMN, "Metric Label", ReportValueType.TEXT, 1),
            new DynamicReportColumn(METRIC_VALUE_COLUMN, "Metric Value", ReportValueType.TEXT, 2)
    );

    public ReportSnapshotPayload {
        reportView = reportView == null
                ? new DynamicReportView("operations", "Operations Snapshot", OPERATIONS_COLUMNS, List.of())
                : reportView;
    }

    public static ReportSnapshotPayload empty(String reportCode, String reportName) {
        return new ReportSnapshotPayload(null, new DynamicReportView(reportCode, reportName, List.of(), List.of()));
    }

    public static ReportSnapshotPayload fromOperationsReport(OperationsReportView report) {
        return new ReportSnapshotPayload(
                report.outboxEventId(),
                new DynamicReportView(
                        "operations",
                        "Operations Snapshot",
                        OPERATIONS_COLUMNS,
                        List.of(
                                metricRow("activeTables", "Active Tables", ReportValueType.INTEGER, report.activeTables()),
                                metricRow("totalTables", "Total Tables", ReportValueType.INTEGER, report.totalTables()),
                                metricRow("openOrders", "Open Orders", ReportValueType.INTEGER, report.openOrders()),
                                metricRow("readyToServe", "Ready To Serve", ReportValueType.INTEGER, report.readyToServe()),
                                metricRow("kitchenQueue", "Kitchen Queue", ReportValueType.INTEGER, report.kitchenQueue()),
                                metricRow("averageKitchenWaitMinutes", "Average Kitchen Wait (Minutes)", ReportValueType.DURATION_MINUTES, report.averageKitchenWaitMinutes()),
                                metricRow("averageServiceTimeMinutes", "Average Service Time (Minutes)", ReportValueType.DURATION_MINUTES, report.averageServiceTimeMinutes()),
                                metricRow("openLowStockAlerts", "Open Low Stock Alerts", ReportValueType.INTEGER, report.openLowStockAlerts()),
                                metricRow("revenueToday", "Revenue Today", ReportValueType.MONEY, report.revenueToday()),
                                metricRow("transactionsToday", "Transactions Today", ReportValueType.INTEGER, report.transactionsToday()),
                                metricRow("reservationCountToday", "Reservations Today", ReportValueType.INTEGER, report.reservationCountToday()),
                                metricRow("refundsToday", "Refunds Today", ReportValueType.MONEY, report.refundsToday()),
                                jsonMetricRow("revenueTrend", "Revenue Trend", report.revenueTrend()),
                                jsonMetricRow("weeklyRevenue", "Weekly Revenue", report.weeklyRevenue()),
                                jsonMetricRow("topDishes", "Top Dishes", report.topDishes()),
                                jsonMetricRow("peakHours", "Peak Hours", report.peakHours()),
                                jsonMetricRow("categoryRevenue", "Category Revenue", report.categoryRevenue()),
                                jsonMetricRow("kitchenMetrics", "Kitchen Metrics", report.kitchenMetrics())
                        )
                )
        );
    }

    public Optional<DynamicReportCell> metricValue(String metricKey) {
        return reportView.rows().stream()
                .filter(row -> row.cell(METRIC_KEY_COLUMN).map(DynamicReportCell::textValue).orElse("").equals(metricKey))
                .findFirst()
                .flatMap(row -> row.cell(METRIC_VALUE_COLUMN));
    }

    public long longMetric(String metricKey) {
        return metricValue(metricKey)
                .map(cell -> {
                    Object value = cell.typedValue();
                    if (value instanceof Number number) {
                        return number.longValue();
                    }
                    try {
                        return Long.parseLong(cell.displayValue());
                    } catch (NumberFormatException exception) {
                        return 0L;
                    }
                })
                .orElse(0L);
    }

    public String textMetric(String metricKey) {
        return metricValue(metricKey)
                .map(DynamicReportCell::textValue)
                .orElse("");
    }

    private static DynamicReportRow metricRow(String key, String label, ReportValueType valueType, Object value) {
        String displayValue = displayScalarValue(value);
        return new DynamicReportRow(List.of(
                new DynamicReportCell(METRIC_KEY_COLUMN, key, ReportValueType.TEXT, key),
                new DynamicReportCell(METRIC_LABEL_COLUMN, label, ReportValueType.TEXT, label),
                new DynamicReportCell(METRIC_VALUE_COLUMN, displayValue, valueType, value)
        ));
    }

    private static DynamicReportRow jsonMetricRow(String key, String label, Object value) {
        String displayValue = value instanceof List<?> list ? list.size() + " rows" : displayScalarValue(value);
        return new DynamicReportRow(List.of(
                new DynamicReportCell(METRIC_KEY_COLUMN, key, ReportValueType.TEXT, key),
                new DynamicReportCell(METRIC_LABEL_COLUMN, label, ReportValueType.TEXT, label),
                new DynamicReportCell(METRIC_VALUE_COLUMN, displayValue, ReportValueType.JSON, value)
        ));
    }

    private static String displayScalarValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }
}
