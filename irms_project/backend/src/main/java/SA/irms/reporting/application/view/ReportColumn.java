package SA.irms.reporting.application.view;

public record ReportColumn(
        String key,
        String label,
        ReportValueType type,
        int displayOrder
) {
}
