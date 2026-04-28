package SA.irms.reservation.application.query;

import java.time.Instant;
import java.util.UUID;

public record ReservationRow(
        UUID reservationId,
        String guest,
        String phone,
        String email,
        int partySize,
        Instant arrivalAt,
        String status,
        String notes
) {
}
