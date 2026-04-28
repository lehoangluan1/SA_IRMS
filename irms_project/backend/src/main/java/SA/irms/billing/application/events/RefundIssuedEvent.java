package SA.irms.billing.application.events;

import java.util.Map;

import SA.irms.common.events.BaseDomainEvent;
public final class RefundIssuedEvent extends BaseDomainEvent {
    public RefundIssuedEvent(String aggregateId, Map<String, Object> payload) {
        super("RefundIssued", 1, "Refund", aggregateId, payload);
    }
}
