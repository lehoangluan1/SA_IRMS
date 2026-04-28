package SA.irms.reservation.application.port.out;

import java.util.UUID;

public interface ReservationSeatingCommandPort {
    boolean canSeatReservation(UUID reservationId, UUID tableId);

    void assignTable(UUID reservationId, UUID tableId);

    void seatReservation(UUID reservationId, UUID tableId);

    void updateWaitlistStatus(UUID waitlistEntryId, String status);
}
