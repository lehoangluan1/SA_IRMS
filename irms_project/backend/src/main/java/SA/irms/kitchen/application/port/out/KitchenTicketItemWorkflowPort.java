package SA.irms.kitchen.application.port.out;

import java.util.UUID;
import SA.irms.kitchen.application.query.KitchenTicketItemContext;

public interface KitchenTicketItemWorkflowPort {
    void moveItemToCooking(KitchenTicketItemContext itemContext, UUID actorUserId, String correlationId);
    void markItemReady(KitchenTicketItemContext itemContext);
    void backfillInventoryDeduction(KitchenTicketItemContext itemContext, UUID actorUserId);
}
