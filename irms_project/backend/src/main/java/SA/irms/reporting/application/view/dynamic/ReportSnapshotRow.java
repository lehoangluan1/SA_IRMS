package SA.irms.reporting.application.view.dynamic;

import java.time.Instant;
import java.util.UUID;

public record ReportSnapshotRow(
        UUID snapshotId,
        String type,
        Instant periodStart,
        Instant periodEnd,
        Instant generatedAt,
        ReportSnapshotData payload
) {
}
