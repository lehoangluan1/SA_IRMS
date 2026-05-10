package SA.irms.reporting.application;

import SA.irms.reporting.application.view.ReportCellView;
import SA.irms.reporting.application.view.ReportColumn;
import SA.irms.reporting.application.view.ReportExportView;
import SA.irms.reporting.application.view.ReportMetricView;
import SA.irms.reporting.application.view.ReportTableRowView;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class PdfReportExporter implements ReportExporter {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'")
            .withZone(ZoneOffset.UTC);

    @Override
    public String fileExtension() {
        return "pdf";
    }

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_PDF;
    }

    @Override
    public byte[] export(ReportExportView report) {
        List<String> lines = new ArrayList<>();
        lines.add("IRMS " + pretty(report.type()) + " Report");
        lines.add("Generated: " + DATE_TIME_FORMATTER.format(report.generatedAt()));
        lines.add("Period: " + DATE_TIME_FORMATTER.format(report.periodStart()) + " to " + DATE_TIME_FORMATTER.format(report.periodEnd()));
        lines.add("");
        if (!report.metrics().isEmpty()) {
            for (ReportMetricView metric : report.metrics()) {
                lines.add(metric.label() + ": " + metric.displayValue());
            }
            lines.add("");
        }
        lines.add(String.join(" | ", report.columns().stream().map(ReportColumn::label).toList()));
        for (ReportTableRowView row : report.rows()) {
            lines.add(String.join(" | ", row.cells().stream().map(ReportCellView::displayValue).toList()));
        }
        return renderPdf(lines);
    }

    private String pretty(String type) {
        String normalized = type.replace('-', ' ');
        return normalized.substring(0, 1).toUpperCase() + normalized.substring(1);
    }

    private byte[] renderPdf(List<String> lines) {
        StringBuilder content = new StringBuilder();
        content.append("BT\n");
        content.append("/F1 12 Tf\n");
        content.append("50 780 Td\n");
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                content.append("0 -16 Td\n");
            }
            content.append('(').append(escape(lines.get(index))).append(") Tj\n");
        }
        content.append("ET");

        byte[] contentBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);
        List<byte[]> objects = new ArrayList<>();
        objects.add(bytes("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"));
        objects.add(bytes("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n"));
        objects.add(bytes("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n"));
        objects.add(bytes("4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n"));
        objects.add(bytes("5 0 obj << /Length " + contentBytes.length + " >> stream\n"));
        objects.add(contentBytes);
        objects.add(bytes("\nendstream endobj\n"));

        List<Integer> offsets = new ArrayList<>();
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        for (int index = 0; index < 4; index++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length);
            pdf.append(new String(objects.get(index), StandardCharsets.ISO_8859_1));
        }
        offsets.add(pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length);
        pdf.append(new String(objects.get(4), StandardCharsets.ISO_8859_1));
        pdf.append(new String(objects.get(5), StandardCharsets.ISO_8859_1));
        pdf.append(new String(objects.get(6), StandardCharsets.ISO_8859_1));

        int xrefOffset = pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        pdf.append("xref\n0 6\n");
        pdf.append("0000000000 65535 f \n");
        for (Integer offset : offsets) {
            pdf.append(String.format("%010d 00000 n %n", offset));
        }
        pdf.append("trailer << /Size 6 /Root 1 0 R >>\n");
        pdf.append("startxref\n");
        pdf.append(xrefOffset).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }
}
