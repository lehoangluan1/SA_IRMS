package SA.irms.identity.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class IdentityAuditRepository {
    private final IdentityAuditReadRepository readRepository;
    private final IdentityAuditWriteRepository writeRepository;

    public IdentityAuditRepository(
            IdentityAuditReadRepository readRepository,
            IdentityAuditWriteRepository writeRepository
    ) {
        this.readRepository = readRepository;
        this.writeRepository = writeRepository;
    }

    public List<IdentityRepository.AuditRow> searchAudit(
            String searchTerm,
            String entityFilter,
            String actorRole,
            LocalDate startDate,
            LocalDate endDate,
            Integer limit
    ) {
        return readRepository.searchAudit(searchTerm, entityFilter, actorRole, startDate, endDate, limit);
    }

    @Transactional
    public void updateAuditFollowUp(UUID auditLogId, boolean followUp) {
        writeRepository.updateAuditFollowUp(auditLogId, followUp);
    }

    @Transactional
    public boolean insertAudit(IdentityRepository.AuditCommand command) {
        return writeRepository.insertAudit(command);
    }
}
