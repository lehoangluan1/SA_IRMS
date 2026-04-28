package SA.irms.reporting.application.view;

import java.time.Instant;
import java.util.List;

public record ReportExportView(
        String type,
        Instant generatedAt,
        Instant periodStart,
        Instant periodEnd,
        List<ReportColumn> columns,
        List<ReportTableRowView> rows,
        List<ReportMetricView> metrics
) {
    public ReportExportView {
        columns = List.copyOf(columns == null ? List.of() : columns);
        rows = List.copyOf(rows == null ? List.of() : rows);
        metrics = List.copyOf(metrics == null ? List.of() : metrics);
    }
}
