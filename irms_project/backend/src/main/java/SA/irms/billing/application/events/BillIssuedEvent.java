package SA.irms.billing.application.events;

import java.util.Map;

import SA.irms.common.events.BaseDomainEvent;
public final class BillIssuedEvent extends BaseDomainEvent {
    public BillIssuedEvent(String aggregateId, Map<String, Object> payload) {
        super("BillIssued", 1, "Bill", aggregateId, payload);
    }
}
