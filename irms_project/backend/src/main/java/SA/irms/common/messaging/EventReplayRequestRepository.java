package SA.irms.common.messaging;

import java.util.UUID;

interface EventReplayRequestRepository {
    void enqueueOutboxReplay(UUID eventId, String reason);

    void enqueueConsumerReplay(UUID eventId, String consumerName, String reason);
}
