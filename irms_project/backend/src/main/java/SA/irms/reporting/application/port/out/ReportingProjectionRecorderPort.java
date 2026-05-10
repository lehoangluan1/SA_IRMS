package SA.irms.reporting.application.port.out;

import SA.irms.common.events.EventEnvelope;

public interface ReportingProjectionRecorderPort {
    void recordEventProjection(EventEnvelope envelope);
}
