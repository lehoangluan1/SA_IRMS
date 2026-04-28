package SA.irms.reporting.application.view;

import java.util.List;

public record ReportTableRowView(List<ReportCellView> cells) {
    public ReportTableRowView {
        cells = List.copyOf(cells == null ? List.of() : cells);
    }
}
