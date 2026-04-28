package SA.irms.kitchen.application.port.out;

import java.util.UUID;

public interface KitchenTicketCancellationCommandPort {
    void blockOpenItems(UUID ticketId, String reason);

    void blockTicket(UUID ticketId, String reason);
}
