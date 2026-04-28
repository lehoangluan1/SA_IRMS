package SA.irms.reservation.application.port.out;

import java.util.List;
import java.util.UUID;

public interface ReservationWaitlistCommandPort {
    UUID createWaitlistEntry(UUID branchId, String name, String phone, String email, int party, int quotedWaitMinutes, String notes);

    void markNotified(UUID waitlistEntryId, int maxHoldMinutes);

    void markSkipped(UUID waitlistEntryId);

    int nextPriority();

    void updatePriority(UUID waitlistEntryId, int priority);

    List<UUID> loadExpiredNotifiedWaitlistIds();

    boolean expireNotifiedWaitlistEntry(UUID waitlistEntryId);

    UUID createTableSession(UUID tableId, UUID serverUserId, int guestCount, String correlationId);

    void markTableOccupied(UUID tableId);

    void markWaitlistSeated(UUID waitlistEntryId);

    void createWaitlistTableAssignment(UUID waitlistEntryId, UUID tableId, UUID tableSessionId);
}
