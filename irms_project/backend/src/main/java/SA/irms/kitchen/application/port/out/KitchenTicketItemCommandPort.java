package SA.irms.kitchen.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface KitchenTicketItemCommandPort {
    void moveItemToCooking(UUID ticketItemId, Instant nextActionAt);

    void markItemReady(UUID ticketItemId);

    void blockItem(UUID ticketItemId, String reason);

    void markInventoryDeducted(UUID ticketItemId);
}
