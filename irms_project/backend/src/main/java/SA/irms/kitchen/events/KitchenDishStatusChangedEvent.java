package SA.irms.kitchen.events;

import java.util.Map;
import java.util.UUID;

public record KitchenDishStatusChangedEvent(
        UUID ticketItemId,
        UUID handoffId,
        String type,
        String sourceStation,
        Map<String, Object> details
) {
}
