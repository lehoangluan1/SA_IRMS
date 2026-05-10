package SA.irms.common.audit;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import SA.irms.common.outbox.OutboxEventPublisher;
import SA.irms.common.events.ServiceEventTypes;

/**
 * Shared audit intent recorder for non-identity domain services.
 *
 * <p>Domain services must not write final audit tables directly. They publish a durable
 * audit intent in the same source transaction; the identity-audit service owns final
 * materialization into audit_logs/audit_details.</p>
 */
@Service
public class AuditRecorder {
    private final OutboxEventPublisher outboxEventPublisher;
    private final Clock clock;

    public AuditRecorder(OutboxEventPublisher outboxEventPublisher, Clock clock) {
        this.outboxEventPublisher = outboxEventPublisher;
        this.clock = clock;
    }

    public void record(
            UUID actorUserId,
            String action,
            String entityType,
            String entityId,
            String correlationId,
            String reason,
            boolean followUp,
            String ipAddress,
            Map<String, Object> beforePayload,
            Map<String, Object> afterPayload
    ) {
        UUID auditIntentId = UUID.randomUUID();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("auditIntentId", auditIntentId.toString());
        payload.put("actorUserId", actorUserId == null ? "" : actorUserId.toString());
        payload.put("action", nullToEmpty(action));
        payload.put("entityType", nullToEmpty(entityType));
        payload.put("entityId", nullToEmpty(entityId));
        payload.put("recordedAt", Instant.now(clock).toString());
        payload.put("reason", nullToEmpty(reason));
        payload.put("followUp", followUp);
        payload.put("ipAddress", nullToEmpty(ipAddress));
        payload.put("beforePayload", beforePayload == null ? Map.of() : beforePayload);
        payload.put("afterPayload", afterPayload == null ? Map.of() : afterPayload);
        outboxEventPublisher.publish(
                ServiceEventTypes.AUDIT_RECORDING_REQUESTED,
                "AuditIntent",
                auditIntentId.toString(),
                payload,
                correlationId
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
