package SA.irms.kitchen.application.port.out;

import java.util.UUID;

public interface KitchenTicketQueryRepository {
    SA.irms.kitchen.application.view.KitchenViews.KitchenOverview load(String stationFilter);
    SA.irms.kitchen.application.view.KitchenViews.TicketView findTicket(UUID ticketId);
}
