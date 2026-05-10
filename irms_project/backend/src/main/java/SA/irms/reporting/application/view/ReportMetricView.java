package SA.irms.reporting.application.view;

public record ReportMetricView(
        String key,
        String label,
        ReportValueType valueType,
        String displayValue
) {
    public ReportMetricView {
        key = key == null ? "" : key;
        label = label == null ? key : label;
        valueType = valueType == null ? ReportValueType.TEXT : valueType;
        displayValue = displayValue == null ? "" : displayValue;
    }
}
