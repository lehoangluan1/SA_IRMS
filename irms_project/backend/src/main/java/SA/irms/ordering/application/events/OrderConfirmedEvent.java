package SA.irms.ordering.application.events;

import java.util.Map;

import SA.irms.common.events.BaseDomainEvent;
public final class OrderConfirmedEvent extends BaseDomainEvent {
    public OrderConfirmedEvent(String aggregateId, Map<String, Object> payload) {
        super("OrderConfirmed", 1, "Order", aggregateId, payload);
    }
}
