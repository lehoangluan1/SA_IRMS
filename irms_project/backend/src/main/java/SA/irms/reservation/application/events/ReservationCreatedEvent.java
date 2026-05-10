package SA.irms.reservation.application.events;

import java.util.Map;

import SA.irms.common.events.BaseDomainEvent;
public final class ReservationCreatedEvent extends BaseDomainEvent {
    public ReservationCreatedEvent(String aggregateId, Map<String, Object> payload) {
        super("ReservationCreated", 1, "Reservation", aggregateId, payload);
    }
}
