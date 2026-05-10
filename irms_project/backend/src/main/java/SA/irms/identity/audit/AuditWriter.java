package SA.irms.identity.audit;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import SA.irms.common.outbox.OutboxEventPublisher;
import SA.irms.common.events.ServiceEventTypes;
import SA.irms.identity.persistence.IdentityAuditRepository;
import SA.irms.identity.persistence.IdentityRepository;

@Service
class AuditWriter {
    private final IdentityAuditRepository identityAuditRepository;
    private final AuditDetailBuilder detailBuilder;
    private final Clock clock;
    private final OutboxEventPublisher outboxEventPublisher;

    AuditWriter(
            IdentityAuditRepository identityAuditRepository,
            AuditDetailBuilder detailBuilder,
            Clock clock,
            OutboxEventPublisher outboxEventPublisher
    ) {
        this.identityAuditRepository = identityAuditRepository;
        this.detailBuilder = detailBuilder;
        this.clock = clock;
        this.outboxEventPublisher = outboxEventPublisher;
    }

    void record(
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
        List<IdentityRepository.AuditDetailCommand> details = detailBuilder.buildDetails(beforePayload, afterPayload);
        UUID auditLogId = UUID.randomUUID();
        identityAuditRepository.insertAudit(new IdentityRepository.AuditCommand(
                auditLogId,
                null,
                actorUserId,
                action,
                entityType,
                entityId,
                Instant.now(clock),
                correlationId,
                reason,
                followUp,
                ipAddress,
                beforePayload,
                afterPayload,
                details
        ));
        if (followUp) {
            outboxEventPublisher.publish(
                    ServiceEventTypes.AUDIT_FOLLOW_UP_REQUESTED,
                    "AuditLog",
                    auditLogId.toString(),
                    Map.of(
                            "auditLogId", auditLogId.toString(),
                            "action", action,
                            "entityType", entityType,
                            "entityId", entityId,
                            "reason", reason == null ? "" : reason
                    ),
                    correlationId
            );
        }
    }
}
