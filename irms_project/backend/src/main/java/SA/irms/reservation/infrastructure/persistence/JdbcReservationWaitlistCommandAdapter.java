package SA.irms.reservation.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import SA.irms.reservation.application.port.out.ReservationWaitlistCommandPort;

@Repository
public class JdbcReservationWaitlistCommandAdapter implements ReservationWaitlistCommandPort {
    private final JdbcWaitlistCommandRepository waitlistCommandRepository;
    private final JdbcReservationTableSessionRepository tableSessionRepository;
    private final JdbcWaitlistAssignmentRepository waitlistAssignmentRepository;

    public JdbcReservationWaitlistCommandAdapter(
            JdbcWaitlistCommandRepository waitlistCommandRepository,
            JdbcReservationTableSessionRepository tableSessionRepository,
            JdbcWaitlistAssignmentRepository waitlistAssignmentRepository
    ) {
        this.waitlistCommandRepository = waitlistCommandRepository;
        this.tableSessionRepository = tableSessionRepository;
        this.waitlistAssignmentRepository = waitlistAssignmentRepository;
    }

    @Override
    public UUID createWaitlistEntry(UUID branchId, String name, String phone, String email, int party, int quotedWaitMinutes, String notes) {
        return waitlistCommandRepository.createWaitlistEntry(branchId, name, phone, email, party, quotedWaitMinutes, notes);
    }

    @Override
    public void markNotified(UUID waitlistEntryId, int maxHoldMinutes) {
        waitlistCommandRepository.markNotified(waitlistEntryId, maxHoldMinutes);
    }

    @Override
    public void markSkipped(UUID waitlistEntryId) {
        waitlistCommandRepository.markSkipped(waitlistEntryId);
    }

    @Override
    public int nextPriority() {
        return waitlistCommandRepository.nextPriority();
    }

    @Override
    public void updatePriority(UUID waitlistEntryId, int priority) {
        waitlistCommandRepository.updatePriority(waitlistEntryId, priority);
    }

    @Override
    public List<UUID> loadExpiredNotifiedWaitlistIds() {
        return waitlistCommandRepository.loadExpiredNotifiedWaitlistIds();
    }

    @Override
    public boolean expireNotifiedWaitlistEntry(UUID waitlistEntryId) {
        return waitlistCommandRepository.expireNotifiedWaitlistEntry(waitlistEntryId);
    }

    @Override
    public UUID createTableSession(UUID tableId, UUID serverUserId, int guestCount, String correlationId) {
        return tableSessionRepository.createTableSession(tableId, serverUserId, guestCount, correlationId);
    }

    @Override
    public void markTableOccupied(UUID tableId) {
        tableSessionRepository.markTableOccupied(tableId);
    }

    @Override
    public void markWaitlistSeated(UUID waitlistEntryId) {
        waitlistCommandRepository.markWaitlistSeated(waitlistEntryId);
    }

    @Override
    public void createWaitlistTableAssignment(UUID waitlistEntryId, UUID tableId, UUID tableSessionId) {
        waitlistAssignmentRepository.createWaitlistTableAssignment(waitlistEntryId, tableId, tableSessionId);
    }
}
