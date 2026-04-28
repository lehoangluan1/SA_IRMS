package SA.irms.billing.application.events;

import java.util.Map;

import SA.irms.common.events.BaseDomainEvent;
public final class PaymentCompletedEvent extends BaseDomainEvent {
    public PaymentCompletedEvent(String aggregateId, Map<String, Object> payload) {
        super("PaymentCompleted", 1, "Payment", aggregateId, payload);
    }
}
