package SA.irms.reporting.application.support;

import SA.irms.reporting.application.view.OperationsReportView;
import SA.irms.reporting.application.view.ReportCellView;
import SA.irms.reporting.application.view.ReportColumn;
import SA.irms.reporting.application.view.ReportExportView;
import SA.irms.reporting.application.view.ReportMetricView;
import SA.irms.reporting.application.view.ReportTableRowView;
import SA.irms.reporting.application.view.ReportValueType;
import SA.irms.reporting.application.view.TabularReportRow;
import SA.irms.reporting.application.view.TabularReportView;
import SA.irms.reporting.application.view.dynamic.DynamicReportCell;
import SA.irms.reporting.application.view.dynamic.DynamicReportColumn;
import SA.irms.reporting.application.view.dynamic.DynamicReportRow;
import SA.irms.reporting.application.view.dynamic.DynamicReportView;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReportTableViewFactory {
    private static final List<ReportColumn> OPERATIONS_COLUMNS = List.of(
            new ReportColumn("metric", "Metric", ReportValueType.TEXT, 0),
            new ReportColumn("value", "Value", ReportValueType.TEXT, 1)
    );

    public ReportExportView create(TabularReportView<? extends TabularReportRow> report) {
        List<ReportTableRowView> rows = report.rows().stream()
                .map(row -> new ReportTableRowView(row.cells()))
                .toList();
        return new ReportExportView(
                report.type(),
                report.generatedAt(),
                report.periodStart(),
                report.periodEnd(),
                report.columns(),
                rows,
                report.metrics()
        );
    }

    public ReportExportView create(OperationsReportView report) {
        List<ReportTableRowView> rows = report.metrics().stream()
                .map(this::metricRow)
                .toList();
        return new ReportExportView(
                "operations",
                report.generatedAt(),
                report.periodStart(),
                report.periodEnd(),
                OPERATIONS_COLUMNS,
                rows,
                report.metrics()
        );
    }

    public ReportExportView create(DynamicReportView report) {
        List<ReportColumn> columns = report.columns().stream()
                .sorted(Comparator.comparingInt(DynamicReportColumn::displayOrder))
                .map(column -> new ReportColumn(column.key(), column.label(), column.valueType(), column.displayOrder()))
                .toList();
        List<ReportTableRowView> rows = report.rows().stream()
                .map(this::toRowView)
                .toList();
        return new ReportExportView(
                report.reportCode(),
                java.time.Instant.now(),
                java.time.Instant.now(),
                java.time.Instant.now(),
                columns,
                rows,
                List.of()
        );
    }

    private ReportTableRowView toRowView(DynamicReportRow row) {
        List<ReportCellView> cells = row.cells().stream()
                .map(this::toCellView)
                .toList();
        return new ReportTableRowView(cells);
    }

    private ReportCellView toCellView(DynamicReportCell cell) {
        return new ReportCellView(cell.columnKey(), cell.displayValue(), cell.valueType());
    }

    private ReportTableRowView metricRow(ReportMetricView metric) {
        return new ReportTableRowView(List.of(
                new ReportCellView("metric", metric.label(), ReportValueType.TEXT),
                new ReportCellView("value", metric.displayValue(), metric.valueType())
        ));
    }
}
