package SA.irms.ordering.application.events;

import java.util.Map;

import SA.irms.common.events.BaseDomainEvent;
public final class MenuItemAvailabilityChangedEvent extends BaseDomainEvent {
    public MenuItemAvailabilityChangedEvent(String aggregateId, Map<String, Object> payload) {
        super("MenuItemAvailabilityChanged", 1, "MenuItem", aggregateId, payload);
    }
}
