package SA.irms.reporting.application.view.dynamic;

import java.util.List;
import java.util.Optional;

public record DynamicReportRow(List<DynamicReportCell> cells) {
    public DynamicReportRow {
        cells = List.copyOf(cells == null ? List.of() : cells);
    }

    public Optional<DynamicReportCell> cell(String columnKey) {
        return cells.stream()
                .filter(cell -> cell.columnKey().equals(columnKey))
                .findFirst();
    }
}
