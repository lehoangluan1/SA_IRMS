package SA.irms.reservation.application.port.out;

import java.util.UUID;

public interface ReservationCheckInCommandPort {
    UUID openReservationSession(UUID reservationId, UUID tableId, UUID serverUserId, int guestCount, String correlationId);

    void markTableOccupied(UUID tableId);

    void markReservationSeated(UUID reservationId, int actualPartySize);

    void linkReservationAssignmentSession(UUID reservationId, UUID tableId, UUID sessionId);
}
