package SA.irms.reporting.application.view.dynamic;

import java.util.List;
import java.util.Optional;

public record DynamicReportView(
        String reportCode,
        String reportName,
        List<DynamicReportColumn> columns,
        List<DynamicReportRow> rows
) {
    public DynamicReportView {
        reportCode = reportCode == null ? "dynamic-report" : reportCode;
        reportName = reportName == null || reportName.isBlank() ? reportCode : reportName;
        columns = List.copyOf(columns == null ? List.of() : columns);
        rows = List.copyOf(rows == null ? List.of() : rows);
    }

    public Optional<DynamicReportColumn> column(String key) {
        return columns.stream().filter(column -> column.key().equals(key)).findFirst();
    }
}
