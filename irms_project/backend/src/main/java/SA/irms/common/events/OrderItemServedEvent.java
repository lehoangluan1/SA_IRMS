package SA.irms.common.events;

import java.util.UUID;

public record OrderItemServedEvent(
        UUID orderItemId,
        UUID actorUserId,
        String correlationId
) {
}
