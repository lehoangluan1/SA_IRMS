package SA.irms.identity.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface IdentityEventOperationsPort {
    List<FailedOutboxEventRow> failedOutboxEvents();

    List<DeadLetterEventRow> deadLetterEvents();

    record FailedOutboxEventRow(
            UUID eventId,
            String eventType,
            String aggregateType,
            String aggregateId,
            String producerService,
            String exchangeName,
            String routingKey,
            int retryCount,
            String lastError,
            Instant createdAt
    ) {
    }

    record DeadLetterEventRow(
            UUID deadLetterId,
            UUID eventId,
            String consumerName,
            String eventType,
            String failureReason,
            String status,
            Instant createdAt
    ) {
    }
}
