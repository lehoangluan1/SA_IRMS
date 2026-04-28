package SA.irms.inventory.application.events;

import java.util.Map;

import SA.irms.common.events.BaseDomainEvent;
public final class LowStockDetectedEvent extends BaseDomainEvent {
    public LowStockDetectedEvent(String aggregateId, Map<String, Object> payload) {
        super("LowStockDetected", 1, "InventoryItem", aggregateId, payload);
    }
}
