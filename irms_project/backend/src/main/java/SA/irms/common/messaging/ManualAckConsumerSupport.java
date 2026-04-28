package SA.irms.common.messaging;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.inbox.InboxEventDeduplicator;

@Component
@ConditionalOnProperty(name = "irms.rabbitmq.enabled", havingValue = "true")
public class ManualAckConsumerSupport {
    private final EventMessageConverter converter;
    private final InboxEventDeduplicator deduplicator;
    private final DeadLetterEventService deadLetterEventService;
    private final RabbitTemplate rabbitTemplate;
    private final int maxRabbitRetries;
    private final long baseRetryDelayMillis;

    public ManualAckConsumerSupport(
            EventMessageConverter converter,
            InboxEventDeduplicator deduplicator,
            DeadLetterEventService deadLetterEventService,
            RabbitTemplate rabbitTemplate,
            @Value("${irms.rabbitmq.consumer.max-retries:5}") int maxRabbitRetries,
            @Value("${irms.rabbitmq.consumer.base-retry-delay-millis:5000}") long baseRetryDelayMillis
    ) {
        this.converter = converter;
        this.deduplicator = deduplicator;
        this.deadLetterEventService = deadLetterEventService;
        this.rabbitTemplate = rabbitTemplate;
        this.maxRabbitRetries = maxRabbitRetries;
        this.baseRetryDelayMillis = baseRetryDelayMillis;
    }

    public void handle(Message message, Channel channel, String consumerName, java.util.function.Consumer<EventEnvelope> handler) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        EventEnvelope envelope = null;
        try {
            envelope = converter.fromMessage(message);
            EventEnvelope finalEnvelope = envelope;
            deduplicator.processOnce(finalEnvelope, consumerName, () -> handler.accept(finalEnvelope));
            channel.basicAck(deliveryTag, false);
        } catch (RuntimeException exception) {
            int retryCount = retryCount(message);
            if (retryCount >= Math.max(1, maxRabbitRetries)) {
                recordAndPublishDeadLetter(message, envelope, consumerName, exception);
                channel.basicAck(deliveryTag, false);
                return;
            }
            publishRetry(message, envelope, consumerName, exception, retryCount + 1);
            channel.basicAck(deliveryTag, false);
        }
    }

    private int retryCount(Message message) {
        Object retryHeader = message.getMessageProperties().getHeaders().get("x-irms-retry-count");
        if (retryHeader instanceof Number retryNumber) {
            return retryNumber.intValue();
        }
        if (retryHeader != null) {
            try {
                return Integer.parseInt(retryHeader.toString());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, rabbitDeathCount(message)));
    }

    @SuppressWarnings("unchecked")
    private long rabbitDeathCount(Message message) {
        Object rawDeath = message.getMessageProperties().getHeaders().get("x-death");
        if (!(rawDeath instanceof List<?> deaths)) {
            return 0;
        }
        long total = 0;
        for (Object death : deaths) {
            if (death instanceof Map<?, ?> deathMap) {
                Object count = deathMap.get("count");
                if (count instanceof Number number) {
                    total += number.longValue();
                }
            }
        }
        return total;
    }

    private void publishRetry(Message message, EventEnvelope envelope, String consumerName, RuntimeException exception, int retryCount) {
        Map<String, Object> headers = new HashMap<>(message.getMessageProperties().getHeaders());
        headers.put("x-irms-retry-count", retryCount);
        headers.put("x-irms-retry-consumer", consumerName);
        headers.put("x-irms-last-failure", exception.getMessage());
        if (envelope != null) {
            headers.put(RabbitMqMessageHeaders.EVENT_ID, envelope.metadata().eventId().toString());
            headers.put(RabbitMqMessageHeaders.EVENT_TYPE, envelope.metadata().eventType());
            headers.put(RabbitMqMessageHeaders.CAUSATION_ID, envelope.metadata().causationId());
            headers.put(RabbitMqMessageHeaders.IDEMPOTENCY_KEY, envelope.metadata().idempotencyKey());
            headers.put("correlationId", envelope.metadata().correlationId());
        }
        message.getMessageProperties().getHeaders().clear();
        message.getMessageProperties().getHeaders().putAll(headers);
        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        message.getMessageProperties().setExpiration(String.valueOf(backoffMillis(retryCount)));
        rabbitTemplate.send(EventRoutingRegistrySupport.RETRY_EXCHANGE, consumerQueueName(message, consumerName) + ".retry", message);
    }

    private long backoffMillis(int retryCount) {
        long multiplier = 1L << Math.min(10, Math.max(0, retryCount - 1));
        return Math.min(baseRetryDelayMillis * multiplier, 300_000L);
    }

    private void recordAndPublishDeadLetter(Message message, EventEnvelope envelope, String consumerName, RuntimeException exception) {
        Map<String, Object> headers = new HashMap<>(message.getMessageProperties().getHeaders());
        headers.put("finalConsumerName", consumerName);
        headers.put("finalFailureReason", exception.getMessage());
        if (envelope == null) {
            deadLetterEventService.recordRaw(consumerName, exception.getMessage(), headers, message.getBody());
        } else {
            deadLetterEventService.record(envelope, consumerName, exception.getMessage(), headers);
        }
        String dlqRoutingKey = consumerQueueName(message, consumerName) + ".dlq";
        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        message.getMessageProperties().setHeader("x-irms-dead-lettered-by", consumerName);
        message.getMessageProperties().setHeader("x-irms-final-failure", exception.getMessage());
        rabbitTemplate.send(EventRoutingRegistrySupport.DLX_EXCHANGE, dlqRoutingKey, message);
    }

    private String consumerQueueName(Message message, String fallbackConsumerName) {
        String consumerQueue = message.getMessageProperties().getConsumerQueue();
        return consumerQueue == null || consumerQueue.isBlank() ? fallbackConsumerName : consumerQueue;
    }

    private static final class EventRoutingRegistrySupport {
        private static final String DLX_EXCHANGE = SA.irms.common.events.EventRoutingRegistry.DLX_EXCHANGE;
        private static final String RETRY_EXCHANGE = SA.irms.common.events.EventRoutingRegistry.RETRY_EXCHANGE;
    }
}
