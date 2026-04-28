package SA.irms.common.messaging;

import java.util.UUID;

interface DeadLetterEventRepository {
    void record(DeadLetterEventRecord record);

    void markReplayRequested(UUID eventId, String consumerName);

    record DeadLetterEventRecord(
            UUID deadLetterId,
            UUID eventId,
            String consumerName,
            String eventType,
            String payloadJson,
            String brokerHeadersJson,
            String failureReason,
            String correlationId,
            String causationId
    ) {
    }
}
