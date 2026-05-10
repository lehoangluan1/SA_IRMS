package SA.irms.reservation.application.port.out;

import SA.irms.reservation.application.query.ReservationRow;
import java.time.LocalDate;
import java.util.UUID;

public interface ReservationQueryRepository {
    SA.irms.reservation.application.view.ReservationViews.ReservationOverview load(LocalDate date);
    SA.irms.reservation.application.view.ReservationViews.ReservationView findReservation(UUID reservationId);
    ReservationRow loadReservationRow(UUID reservationId);
}
