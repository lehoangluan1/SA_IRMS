package SA.irms.reporting.application.view;

import org.springframework.http.MediaType;

public final class ReportingViews {
    private ReportingViews() {}

    public record ExportedReport(String filename, MediaType mediaType, byte[] content) {}
}
