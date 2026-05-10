package SA.irms.reservation.application.port.out;

import java.util.UUID;

import SA.irms.reservation.application.query.ReservationNotificationTarget;

public interface ReservationNotificationTargetRepository {
    ReservationNotificationTarget loadReservationNotificationTarget(UUID reservationId);

    ReservationNotificationTarget loadWaitlistNotificationTarget(UUID waitlistEntryId);
}
