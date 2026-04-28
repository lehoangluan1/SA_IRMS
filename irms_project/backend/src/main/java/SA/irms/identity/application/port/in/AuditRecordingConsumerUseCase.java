package SA.irms.identity.application.port.in;

import SA.irms.common.events.EventEnvelope;

public interface AuditRecordingConsumerUseCase {
    void record(EventEnvelope envelope);
}
