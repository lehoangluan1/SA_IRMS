package SA.irms.kitchen.application.events;

import java.util.Map;

import SA.irms.common.events.BaseDomainEvent;
public final class KitchenDishStatusChangedEvent extends BaseDomainEvent {
    public KitchenDishStatusChangedEvent(String aggregateId, Map<String, Object> payload) {
        super("KitchenDishStatusChanged", 1, "KitchenTicketItem", aggregateId, payload);
    }
}
