package SA.irms.reservation.application.workflow;

import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.reservation.application.port.out.ReservationSeatingCommandPort;

@Component
public class AssignTableStep {
    private final ReservationSeatingCommandPort seatingCommandPort;

    public AssignTableStep(ReservationSeatingCommandPort seatingCommandPort) {
        this.seatingCommandPort = seatingCommandPort;
    }

    public void assign(UUID reservationId, UUID tableId) {
        seatingCommandPort.assignTable(reservationId, tableId);
    }
}
