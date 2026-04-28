package SA.irms.reporting.application;

import SA.irms.reporting.application.view.ReportExportView;
import org.springframework.http.MediaType;

public interface ReportExporter {
    String fileExtension();

    MediaType mediaType();

    byte[] export(ReportExportView report);
}
