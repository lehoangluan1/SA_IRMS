package SA.irms.kitchen.application.port.out;

import SA.irms.kitchen.application.KitchenOrderItemCommand;
import java.util.UUID;

public interface KitchenOrderRoutingRepository {
    long countTicketItems(UUID orderItemId);

    UUID createRoutePlan(UUID orderId, String notes);

    UUID resolveStationId(String stationKind);

    UUID createTicket(UUID orderId, UUID routePlanId, UUID stationId);

    int createTicketItem(UUID ticketId, KitchenOrderItemCommand item);

    void deleteEmptyTicket(UUID ticketId);

    void deleteEmptyRoutePlan(UUID routePlanId);

    void holdOrderItemForService(UUID orderItemId);

    void releaseHeldOrderItem(UUID orderItemId);

    void blockOrderItem(UUID orderItemId, String reason);
}
