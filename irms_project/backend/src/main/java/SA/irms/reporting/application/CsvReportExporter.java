package SA.irms.reporting.application;

import SA.irms.reporting.application.view.ReportCellView;
import SA.irms.reporting.application.view.ReportColumn;
import SA.irms.reporting.application.view.ReportExportView;
import SA.irms.reporting.application.view.ReportTableRowView;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class CsvReportExporter implements ReportExporter {
    @Override
    public String fileExtension() {
        return "csv";
    }

    @Override
    public MediaType mediaType() {
        return new MediaType("text", "csv");
    }

    @Override
    public byte[] export(ReportExportView report) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.join(",", report.columns().stream().map(ReportColumn::label).map(this::escape).toList())).append('\n');
        for (ReportTableRowView row : report.rows()) {
            builder.append(String.join(",", row.cells().stream().map(ReportCellView::displayValue).map(this::escape).toList())).append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
