package SA.irms.kitchen.application.events;

import java.util.Map;

import SA.irms.common.events.BaseDomainEvent;
public final class KitchenTicketCreatedEvent extends BaseDomainEvent {
    public KitchenTicketCreatedEvent(String aggregateId, Map<String, Object> payload) {
        super("KitchenTicketCreated", 1, "KitchenTicket", aggregateId, payload);
    }
}
