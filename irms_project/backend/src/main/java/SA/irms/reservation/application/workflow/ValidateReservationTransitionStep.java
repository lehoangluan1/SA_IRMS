package SA.irms.reservation.application.workflow;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.common.error.ConflictException;
import SA.irms.reservation.application.port.out.ReservationSeatingCommandPort;

@Component
public class ValidateReservationTransitionStep {
    private final ReservationSeatingCommandPort seatingCommandPort;

    public ValidateReservationTransitionStep(ReservationSeatingCommandPort seatingCommandPort) {
        this.seatingCommandPort = seatingCommandPort;
    }

    public Map<String, Object> validate(UUID reservationId, UUID tableId) {
        if (!seatingCommandPort.canSeatReservation(reservationId, tableId)) {
            throw new ConflictException("Reservation cannot be seated from its current state.");
        }
        return Map.of("reservationId", reservationId.toString(), "tableId", tableId.toString());
    }
}
