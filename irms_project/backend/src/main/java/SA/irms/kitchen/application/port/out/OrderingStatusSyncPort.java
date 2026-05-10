package SA.irms.kitchen.application.port.out;

import java.util.List;
import java.util.UUID;

import SA.irms.kitchen.application.port.out.OrderItemLineStatusUpdate;

public interface OrderingStatusSyncPort {
    void refreshOrder(UUID orderId);

    void applyKitchenLineStatuses(UUID orderId, List<OrderItemLineStatusUpdate> lineStatuses);
}
