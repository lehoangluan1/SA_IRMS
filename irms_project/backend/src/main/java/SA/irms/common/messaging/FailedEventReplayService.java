package SA.irms.common.messaging;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.outbox.OutboxEventRepository;

@Service
public class FailedEventReplayService {
    private final OutboxEventRepository outboxEventRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final DeadLetterEventRepository deadLetterEventRepository;
    private final EventReplayRequestRepository eventReplayRequestRepository;

    public FailedEventReplayService(
            OutboxEventRepository outboxEventRepository,
            ProcessedEventRepository processedEventRepository,
            DeadLetterEventRepository deadLetterEventRepository,
            EventReplayRequestRepository eventReplayRequestRepository
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.processedEventRepository = processedEventRepository;
        this.deadLetterEventRepository = deadLetterEventRepository;
        this.eventReplayRequestRepository = eventReplayRequestRepository;
    }

    @Transactional
    public void replayOutboxEvent(UUID eventId, String reason) {
        outboxEventRepository.resetForReplay(eventId);
        eventReplayRequestRepository.enqueueOutboxReplay(eventId, reason);
    }

    @Transactional
    public void replayConsumerEvent(UUID eventId, String consumerName, String reason) {
        processedEventRepository.delete(eventId, consumerName);
        deadLetterEventRepository.markReplayRequested(eventId, consumerName);
        eventReplayRequestRepository.enqueueConsumerReplay(eventId, consumerName, reason);
    }
}
