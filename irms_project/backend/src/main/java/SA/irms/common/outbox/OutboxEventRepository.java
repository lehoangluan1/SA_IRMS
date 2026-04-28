package SA.irms.common.outbox;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import SA.irms.common.events.EventMetadata;
import SA.irms.common.events.EventRouting;

@Repository("outboxEventRepository")
public class OutboxEventRepository {
    private final OutboxEventAppender appender;
    private final OutboxEventPoller poller;
    private final OutboxEventStatusUpdater statusUpdater;

    public OutboxEventRepository(
            OutboxEventAppender appender,
            OutboxEventPoller poller,
            OutboxEventStatusUpdater statusUpdater
    ) {
        this.appender = appender;
        this.poller = poller;
        this.statusUpdater = statusUpdater;
    }

    public UUID storePending(EventMetadata metadata, Map<String, Object> payload, EventRouting routing) {
        return appender.storePending(metadata, payload, routing);
    }

    public Optional<UUID> findEventIdByIdempotencyKey(String idempotencyKey) {
        return appender.findEventIdByIdempotencyKey(idempotencyKey);
    }

    public List<OutboxEvent> lockPendingForPublish(int limit) {
        return poller.lockPendingForPublish(limit);
    }

    public Optional<OutboxEvent> find(UUID eventId) {
        return poller.find(eventId);
    }

    public void markPublished(UUID eventId) {
        statusUpdater.markPublished(eventId);
    }

    public void markFailed(UUID eventId, Throwable failure, int maxRetries, Duration baseBackoff) {
        statusUpdater.markFailed(eventId, failure, maxRetries, baseBackoff);
    }

    public void resetForReplay(UUID eventId) {
        statusUpdater.resetForReplay(eventId);
    }
}
