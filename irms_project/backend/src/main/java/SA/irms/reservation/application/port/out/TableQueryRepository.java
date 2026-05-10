package SA.irms.reservation.application.port.out;

import SA.irms.reservation.application.query.ActiveTableState;
import java.util.UUID;

public interface TableQueryRepository {
    SA.irms.reservation.application.view.ReservationViews.TableView findTable(UUID tableId);
    ActiveTableState loadActiveTableState(UUID tableId);
    String findSuggestedWaitlistGuest(int tableCapacity);
}
