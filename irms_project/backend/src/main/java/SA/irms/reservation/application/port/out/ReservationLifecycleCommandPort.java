package SA.irms.reservation.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReservationLifecycleCommandPort {
    UUID createContact(String name, String phone, String email);

    UUID createReservation(UUID branchId, UUID contactId, Instant arrivalAt, int partySize, String status, String notes,
                           Instant confirmedAt, Instant holdExpiresAt);

    void updateReservation(UUID reservationId, String notes, int partySize);

    void updateContactNameForReservation(UUID reservationId, String name);

    void confirmReservation(UUID reservationId);

    void markReservationNoShow(UUID reservationId);

    List<UUID> loadOverdueReservationIds(Instant cutoff);

    void cancelExpiredReservation(UUID reservationId, String cancelReason);

    List<UUID> loadUpcomingReminderReservationIds(Instant windowStart, Instant windowEnd, Instant windowFloor);
}
