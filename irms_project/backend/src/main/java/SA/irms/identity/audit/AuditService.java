package SA.irms.identity.audit;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.security.AuthenticatedUser;
import SA.irms.identity.persistence.IdentityAuditRepository;
import SA.irms.identity.persistence.IdentityRepository;

@Service
public class AuditService {
    private final AuditWriter auditWriter;
    private final AuditReader auditReader;
    private final AuditCsvExporter csvExporter;
    private final IdentityAuditRepository identityAuditRepository;

    public AuditService(
            AuditWriter auditWriter,
            AuditReader auditReader,
            AuditCsvExporter csvExporter,
            IdentityAuditRepository identityAuditRepository
    ) {
        this.auditWriter = auditWriter;
        this.auditReader = auditReader;
        this.csvExporter = csvExporter;
        this.identityAuditRepository = identityAuditRepository;
    }

    @Transactional
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
        auditWriter.record(actorUserId, action, entityType, entityId, correlationId, reason, followUp,
                ipAddress, beforePayload, afterPayload);
    }

    public List<IdentityRepository.AuditRow> search(
            String searchTerm,
            String actionFilter,
            String actorRole,
            LocalDate startDate,
            LocalDate endDate,
            Integer limit,
            AuthenticatedUser viewer
    ) {
        return auditReader.search(searchTerm, actionFilter, actorRole, startDate, endDate, limit, viewer);
    }

    @Transactional
    public void setFollowUp(UUID auditLogId, boolean followUp) {
        identityAuditRepository.updateAuditFollowUp(auditLogId, followUp);
    }

    public String exportCsv(List<IdentityRepository.AuditRow> auditRows) {
        return csvExporter.export(auditRows);
    }

    public void recordAccess(
            AuthenticatedUser actor,
            String action,
            String correlationId,
            String ipAddress,
            Map<String, Object> filters
    ) {
        record(
                actor.userId(),
                action,
                "AuditLog",
                "audit-log",
                correlationId,
                "Sensitive audit access was requested.",
                false,
                ipAddress,
                Map.of(),
                filters
        );
    }
}
