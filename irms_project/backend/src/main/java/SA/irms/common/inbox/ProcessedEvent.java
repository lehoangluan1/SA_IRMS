package SA.irms.common.inbox;

import java.time.Instant;
import java.util.UUID;

public record ProcessedEvent(
        UUID eventId,
        String consumerName,
        String eventType,
        Instant processedAt,
        String status,
        String error
) {
}
