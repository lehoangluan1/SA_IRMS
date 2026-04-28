package SA.irms.reservation.application.workflow;

import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.reservation.application.port.out.ReservationSeatingCommandPort;

@Component
public class UpdateWaitlistStep {
    private final ReservationSeatingCommandPort seatingCommandPort;

    public UpdateWaitlistStep(ReservationSeatingCommandPort seatingCommandPort) {
        this.seatingCommandPort = seatingCommandPort;
    }

    public void execute(UUID waitlistEntryId, String status) {
        seatingCommandPort.updateWaitlistStatus(waitlistEntryId, status);
    }
}
