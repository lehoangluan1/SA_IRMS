package SA.irms.reporting.application.view.dynamic;

import SA.irms.reporting.application.view.OperationsReportView;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ReportSnapshotData(
        Map<String, String> metrics,
        List<ReportDataRow> rows
) {
    public ReportSnapshotData {
        metrics = Collections.unmodifiableMap(new LinkedHashMap<>(metrics == null ? Map.of() : metrics));
        rows = List.copyOf(rows == null ? List.of() : rows);
    }

    public static ReportSnapshotData empty() {
        return new ReportSnapshotData(Map.of(), List.of());
    }

    public static ReportSnapshotData fromOperationsReport(OperationsReportView report) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("activeTables", String.valueOf(report.activeTables()));
        values.put("openOrders", String.valueOf(report.openOrders()));
        values.put("readyToServe", String.valueOf(report.readyToServe()));
        values.put("kitchenQueue", String.valueOf(report.kitchenQueue()));
        values.put("averageKitchenWaitMinutes", String.valueOf(report.averageKitchenWaitMinutes()));
        values.put("openLowStockAlerts", String.valueOf(report.openLowStockAlerts()));
        if (report.outboxEventId() != null && !report.outboxEventId().isBlank()) {
            values.put("outboxEventId", report.outboxEventId());
        }
        return new ReportSnapshotData(values, List.of());
    }
}
