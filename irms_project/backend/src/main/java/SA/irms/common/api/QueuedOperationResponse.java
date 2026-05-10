package SA.irms.common.api;

import java.util.UUID;

public record QueuedOperationResponse(
        UUID eventId,
        String consumerName,
        String status
) {
    public static QueuedOperationResponse queued(UUID eventId) {
        return new QueuedOperationResponse(eventId, null, "QUEUED");
    }

    public static QueuedOperationResponse queued(UUID eventId, String consumerName) {
        return new QueuedOperationResponse(eventId, consumerName, "QUEUED");
    }
}
