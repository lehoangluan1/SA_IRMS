package SA.irms.kitchen.application.port.out;

import SA.irms.kitchen.application.query.KitchenTicketItemContext;
import java.time.Instant;
import java.util.UUID;

public interface KitchenTicketWorkflowCommandPort {
    void updateTicketState(UUID ticketId, String persistentStatus, Instant deadline);

    void cancelOpenTicketsForOrder(UUID orderId, String reason);
}
