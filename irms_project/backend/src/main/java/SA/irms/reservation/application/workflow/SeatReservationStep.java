package SA.irms.reservation.application.workflow;

import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.reservation.application.port.out.ReservationSeatingCommandPort;

@Component
public class SeatReservationStep {
    private final ReservationSeatingCommandPort seatingCommandPort;

    public SeatReservationStep(ReservationSeatingCommandPort seatingCommandPort) {
        this.seatingCommandPort = seatingCommandPort;
    }

    public void seat(UUID reservationId, UUID tableId) {
        seatingCommandPort.seatReservation(reservationId, tableId);
    }
}
