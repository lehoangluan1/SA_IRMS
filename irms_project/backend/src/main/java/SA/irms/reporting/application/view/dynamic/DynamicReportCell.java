package SA.irms.reporting.application.view.dynamic;

import SA.irms.reporting.application.view.ReportValueType;

public record DynamicReportCell(
        String columnKey,
        String displayValue,
        ReportValueType valueType,
        Object typedValue
) {
    public DynamicReportCell {
        columnKey = columnKey == null ? "" : columnKey;
        displayValue = displayValue == null ? "" : displayValue;
        valueType = valueType == null ? ReportValueType.TEXT : valueType;
    }

    public String textValue() {
        if (typedValue == null) {
            return displayValue;
        }
        return String.valueOf(typedValue);
    }
}
