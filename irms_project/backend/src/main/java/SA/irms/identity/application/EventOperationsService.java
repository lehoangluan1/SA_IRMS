package SA.irms.identity.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.identity.application.port.out.IdentityEventOperationsPort;
import SA.irms.identity.application.view.IdentityViews;
import SA.irms.common.messaging.FailedEventReplayService;

@Service
public class EventOperationsService {
    private final IdentityEventOperationsPort repository;
    private final FailedEventReplayService replayService;

    public EventOperationsService(IdentityEventOperationsPort repository, FailedEventReplayService replayService) {
        this.repository = repository;
        this.replayService = replayService;
    }

    public List<IdentityViews.FailedOutboxEventView> failedOutbox() {
        return repository.failedOutboxEvents().stream()
                .map(row -> new IdentityViews.FailedOutboxEventView(row.eventId(), row.eventType(), row.aggregateType(), row.aggregateId(),
                        row.producerService(), row.exchangeName(), row.routingKey(), row.retryCount(), row.lastError(), row.createdAt()))
                .toList();
    }

    public List<IdentityViews.DeadLetterEventView> deadLetters() {
        return repository.deadLetterEvents().stream()
                .map(row -> new IdentityViews.DeadLetterEventView(row.deadLetterId(), row.eventId(), row.consumerName(), row.eventType(),
                        row.failureReason(), row.status(), row.createdAt()))
                .toList();
    }

    @Transactional
    public void replayOutbox(UUID eventId, String reason) {
        replayService.replayOutboxEvent(eventId, reason);
    }

    @Transactional
    public void replayConsumer(UUID eventId, String consumerName, String reason) {
        replayService.replayConsumerEvent(eventId, consumerName, reason);
    }
}
