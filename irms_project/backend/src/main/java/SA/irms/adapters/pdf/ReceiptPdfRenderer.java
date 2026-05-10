package SA.irms.adapters.pdf;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ReceiptPdfRenderer {

    public byte[] render(List<String> lines) {
        List<String> safeLines = lines == null || lines.isEmpty() ? List.of("Receipt") : lines;
        StringBuilder content = new StringBuilder();
        content.append("BT\n");
        content.append("/F1 12 Tf\n");
        content.append("50 780 Td\n");
        for (int index = 0; index < safeLines.size(); index++) {
            if (index > 0) {
                content.append("0 -16 Td\n");
            }
            content.append("(").append(escape(safeLines.get(index))).append(") Tj\n");
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
