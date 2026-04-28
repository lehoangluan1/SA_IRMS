package SA.irms.ordering.application;

import java.util.List;
import java.util.UUID;

public interface KitchenCoordinationPort {
    void queueConfirmedItems(UUID orderId, List<KitchenOrderItemCommand> items, String notes);

    void holdOrderItemForService(UUID orderItemId);

    void releaseHeldOrderItem(UUID orderId, KitchenOrderItemCommand item, String notes);

    void blockOrderItem(UUID orderItemId, String reason);

    record KitchenOrderItemCommand(UUID orderItemId, String station, int quantity) {
    }
}
