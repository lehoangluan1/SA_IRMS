package SA.irms.reporting.application;

import SA.irms.common.events.EventEnvelope;

public interface ReportingProjectionHandler {
    boolean supports(EventEnvelope envelope);

    void project(EventEnvelope envelope);
}
