package SA.irms.common.inbox;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.events.EventEnvelope;

@Service
public class InboxEventDeduplicator {
    private final ProcessedEventRepository repository;

    public InboxEventDeduplicator(ProcessedEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean processOnce(EventEnvelope envelope, String consumerName, Runnable action) {
        UUID eventId = envelope.metadata().eventId();
        if (repository.find(eventId, consumerName)
                .filter(event -> "PROCESSED".equals(event.status()))
                .isPresent()) {
            return false;
        }
        if (!repository.tryStart(envelope, consumerName)) {
            return false;
        }
        try {
            action.run();
            repository.markProcessed(eventId, consumerName);
            return true;
        } catch (RuntimeException exception) {
            repository.markFailed(eventId, consumerName, exception);
            throw exception;
        }
    }
}
