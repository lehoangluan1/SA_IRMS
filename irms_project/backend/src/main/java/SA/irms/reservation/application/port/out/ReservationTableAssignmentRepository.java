package SA.irms.reservation.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import SA.irms.reservation.application.query.TableCandidate;

public interface ReservationTableAssignmentRepository {
    int countActiveWaitlistEntries();

    List<TableCandidate> loadTablesByMinimumCapacity(int partySize);

    Optional<UUID> findAssignedTableId(UUID reservationId);

    void releaseSpecificAssignment(UUID reservationId, UUID tableId, String reason);

    void markTableAvailableIfReserved(UUID tableId);

    void reserveTableForReservation(UUID reservationId, UUID tableId);
}
