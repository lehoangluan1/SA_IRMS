package SA.irms.reservation.application.events;

import java.util.Map;

import SA.irms.common.events.BaseDomainEvent;
public final class ReservationSeatedEvent extends BaseDomainEvent {
    public ReservationSeatedEvent(String aggregateId, Map<String, Object> payload) {
        super("ReservationSeated", 1, "Reservation", aggregateId, payload);
    }
}
