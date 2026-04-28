package SA.irms.reporting.application;

import SA.irms.reporting.application.view.ReportExportView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class JsonReportExporter implements ReportExporter {
    private final ObjectMapper objectMapper;

    public JsonReportExporter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String fileExtension() {
        return "json";
    }

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    public byte[] export(ReportExportView report) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(report);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Report JSON could not be serialized.", exception);
        }
    }
}
