package SA.irms.common.outbox;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.messaging.RabbitMqEventPublisher;
import SA.irms.common.messaging.RabbitMqPublishResult;

@Component
@ConditionalOnProperty(name = "irms.rabbitmq.enabled", havingValue = "true")
public class RabbitMqOutboxRelay implements OutboxRelay {
    private final OutboxEventRepository repository;
    private final RabbitMqEventPublisher publisher;
    private final int batchSize;
    private final int maxRetries;
    private final Duration baseBackoff;

    public RabbitMqOutboxRelay(
            OutboxEventRepository repository,
            RabbitMqEventPublisher publisher,
            @Value("${irms.outbox.rabbit.batch-size:50}") int batchSize,
            @Value("${irms.outbox.rabbit.max-retries:8}") int maxRetries,
            @Value("${irms.outbox.rabbit.base-backoff-seconds:15}") long baseBackoffSeconds
    ) {
        this.repository = repository;
        this.publisher = publisher;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
        this.baseBackoff = Duration.ofSeconds(baseBackoffSeconds);
    }

    @Override
    @Transactional
    @Scheduled(fixedDelayString = "${irms.outbox.rabbit.poll-millis:1000}")
    public int relayPendingEvents() {
        int published = 0;
        for (OutboxEvent event : repository.lockPendingForPublish(batchSize)) {
            RabbitMqPublishResult result = publisher.publish(event);
            if (result.confirmedAndRouted()) {
                repository.markPublished(event.eventId());
                published++;
            } else {
                repository.markFailed(event.eventId(), new RabbitMqPublishException(result.status() + ": " + result.reason()), maxRetries, baseBackoff);
            }
        }
        return published;
    }

    private static final class RabbitMqPublishException extends RuntimeException {
        RabbitMqPublishException(String message) {
            super(message);
        }
    }
}
