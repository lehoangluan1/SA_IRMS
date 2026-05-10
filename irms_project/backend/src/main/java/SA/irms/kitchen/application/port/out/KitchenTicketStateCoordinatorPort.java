package SA.irms.kitchen.application.port.out;

import java.util.UUID;

public interface KitchenTicketStateCoordinatorPort {
    void reconcileTicket(UUID ticketId);
    void reconcileTicketsForOrderItem(UUID orderItemId);
    void refreshOrderForTicket(UUID ticketId);
    void refreshTicketStatus(UUID ticketId);
}
