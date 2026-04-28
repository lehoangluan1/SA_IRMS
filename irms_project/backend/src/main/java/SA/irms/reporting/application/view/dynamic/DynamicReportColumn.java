package SA.irms.reporting.application.view.dynamic;

import SA.irms.reporting.application.view.ReportValueType;

public record DynamicReportColumn(
        String key,
        String label,
        ReportValueType valueType,
        int displayOrder
) {
    public DynamicReportColumn {
        key = key == null ? "" : key;
        label = label == null || label.isBlank() ? key : label;
        valueType = valueType == null ? ReportValueType.TEXT : valueType;
    }
}
