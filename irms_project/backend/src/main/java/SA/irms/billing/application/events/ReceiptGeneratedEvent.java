package SA.irms.billing.application.events;

import java.util.Map;

import SA.irms.common.events.BaseDomainEvent;
public final class ReceiptGeneratedEvent extends BaseDomainEvent {
    public ReceiptGeneratedEvent(String aggregateId, Map<String, Object> payload) {
        super("ReceiptGenerated", 1, "Receipt", aggregateId, payload);
    }
}
