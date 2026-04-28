package SA.irms.kitchen.application.port.out;

import java.util.UUID;

public interface KitchenInventoryConsumptionPort {
    void consumeForKitchenStart(UUID orderItemId, UUID actorUserId, String correlationId);
}
