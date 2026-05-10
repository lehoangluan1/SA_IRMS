package SA.irms.reporting.application.view.dynamic;

import java.time.Instant;

public record ReportSnapshotQuery(
        String reportCode,
        Instant generatedAfter,
        Instant generatedBefore,
        int limit
) {
    public ReportSnapshotQuery {
        limit = limit <= 0 ? 25 : Math.min(limit, 200);
    }
}
