package SA.irms.reporting.application.port.out;

import SA.irms.reporting.application.view.dynamic.DynamicReportSnapshot;
import SA.irms.reporting.application.view.dynamic.ReportSnapshotQuery;
import SA.irms.reporting.application.view.dynamic.ReportSnapshotSummaryView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportSnapshotRepository {
    Optional<DynamicReportSnapshot> findSnapshot(UUID snapshotId);

    Optional<DynamicReportSnapshot> findLatestSnapshot(String reportCode);

    void saveSnapshot(DynamicReportSnapshot snapshot);

    List<ReportSnapshotSummaryView> findSnapshots(ReportSnapshotQuery query);
}
