package SA.irms.inventory.application.events;

import java.util.Map;

import SA.irms.common.events.BaseDomainEvent;
public final class InventoryStockChangedEvent extends BaseDomainEvent {
    public InventoryStockChangedEvent(String aggregateId, Map<String, Object> payload) {
        super("InventoryStockChanged", 1, "InventoryItem", aggregateId, payload);
    }
}
