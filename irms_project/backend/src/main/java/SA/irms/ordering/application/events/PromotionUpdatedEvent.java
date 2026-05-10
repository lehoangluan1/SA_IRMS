package SA.irms.ordering.application.events;

import java.util.Map;

import SA.irms.common.events.BaseDomainEvent;
public final class PromotionUpdatedEvent extends BaseDomainEvent {
    public PromotionUpdatedEvent(String aggregateId, Map<String, Object> payload) {
        super("PromotionUpdated", 1, "Promotion", aggregateId, payload);
    }
}
