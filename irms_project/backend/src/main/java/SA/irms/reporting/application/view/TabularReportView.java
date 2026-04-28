package SA.irms.reporting.application.view;

import java.time.Instant;
import java.util.List;

public interface TabularReportView<R extends TabularReportRow> {
    String type();

    Instant generatedAt();

    Instant periodStart();

    Instant periodEnd();

    List<ReportColumn> columns();

    List<R> rows();

    List<ReportMetricView> metrics();
}
