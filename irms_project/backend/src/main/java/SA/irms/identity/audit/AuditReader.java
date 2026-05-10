package SA.irms.identity.audit;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import SA.irms.common.security.AuthenticatedUser;
import SA.irms.identity.persistence.IdentityAuditRepository;
import SA.irms.identity.persistence.IdentityRepository;

@Service
class AuditReader {
    private final IdentityAuditRepository identityAuditRepository;
    private final AuditMaskingPolicy maskingPolicy;

    AuditReader(IdentityAuditRepository identityAuditRepository, AuditMaskingPolicy maskingPolicy) {
        this.identityAuditRepository = identityAuditRepository;
        this.maskingPolicy = maskingPolicy;
    }

    List<IdentityRepository.AuditRow> search(
            String searchTerm,
            String actionFilter,
            String actorRole,
            LocalDate startDate,
            LocalDate endDate,
            Integer limit,
            AuthenticatedUser viewer
    ) {
        List<IdentityRepository.AuditRow> auditRows = identityAuditRepository.searchAudit(
                searchTerm,
                actionFilter,
                actorRole,
                startDate,
                endDate,
                limit
        );
        if (viewer.hasRole("admin") || viewer.hasPermission("all")) {
            return auditRows;
        }
        return auditRows.stream()
                .map(maskingPolicy::maskForViewer)
                .toList();
    }
}
