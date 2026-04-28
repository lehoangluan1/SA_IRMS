package SA.irms.reporting.application.view;

public record ReportCellView(
        String columnKey,
        String displayValue,
        ReportValueType valueType
) {
    public ReportCellView {
        displayValue = displayValue == null ? "" : displayValue;
        valueType = valueType == null ? ReportValueType.TEXT : valueType;
    }
}
