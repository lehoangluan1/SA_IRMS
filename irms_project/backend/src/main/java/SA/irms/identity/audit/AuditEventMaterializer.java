package SA.irms.identity.audit;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.identity.application.port.in.AuditRecordingConsumerUseCase;
import SA.irms.common.events.EventEnvelope;

@Service
public class AuditEventMaterializer implements AuditRecordingConsumerUseCase {
    private final AuditService auditService;

    public AuditEventMaterializer(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public void record(EventEnvelope envelope) {
        Map<String, Object> payload = envelope.payload();
        String action = payload.getOrDefault("action", envelope.metadata().eventType()).toString();
        String reason = payload.get("reason") == null ? null : payload.get("reason").toString();
        auditService.record(
                null,
                action,
                envelope.metadata().aggregateType(),
                envelope.metadata().aggregateId(),
                envelope.metadata().correlationId(),
                reason,
                false,
                null,
                Map.of(),
                payload
        );
    }
}
