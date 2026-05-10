package SA.irms.reporting.application.view.dynamic;

import java.time.Instant;
import java.util.UUID;

public record DynamicReportSnapshot(
        UUID snapshotId,
        String reportCode,
        Instant periodStart,
        Instant periodEnd,
        Instant generatedAt,
        ReportSnapshotPayload payload
) {
}
