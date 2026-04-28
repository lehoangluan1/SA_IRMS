package SA.irms.reporting.application.view.dynamic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ReportDataRow(Map<String, String> values) {
    public ReportDataRow {
        values = Collections.unmodifiableMap(new LinkedHashMap<>(values == null ? Map.of() : values));
    }
}
