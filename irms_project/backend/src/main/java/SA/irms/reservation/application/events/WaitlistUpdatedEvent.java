package SA.irms.reservation.application.events;

import java.util.Map;

import SA.irms.common.events.BaseDomainEvent;
public final class WaitlistUpdatedEvent extends BaseDomainEvent {
    public WaitlistUpdatedEvent(String aggregateId, Map<String, Object> payload) {
        super("WaitlistUpdated", 1, "WaitlistEntry", aggregateId, payload);
    }
}
