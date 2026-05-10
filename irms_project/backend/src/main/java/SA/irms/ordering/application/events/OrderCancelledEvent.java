package SA.irms.ordering.application.events;

import java.util.Map;

import SA.irms.common.events.BaseDomainEvent;
public final class OrderCancelledEvent extends BaseDomainEvent {
    public OrderCancelledEvent(String aggregateId, Map<String, Object> payload) {
        super("OrderCancelled", 1, "Order", aggregateId, payload);
    }
}
