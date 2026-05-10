package SA.irms.reporting.application.view.dynamic;

import java.time.Instant;
import java.util.UUID;

public record ReportSnapshotSummaryView(
        UUID snapshotId,
        String reportCode,
        String reportName,
        Instant generatedAt,
        Instant periodStart,
        Instant periodEnd
) {
}
