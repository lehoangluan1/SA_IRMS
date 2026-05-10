package SA.irms.common.messaging;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.EventMetadata;
import SA.irms.common.outbox.OutboxEvent;

@Component
@ConditionalOnProperty(name = "irms.rabbitmq.enabled", havingValue = "true")
public class RabbitMqEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final long confirmTimeoutMillis;
    private final long returnWaitMillis;
    private final ConcurrentMap<String, PublishTracker> trackers = new ConcurrentHashMap<>();

    public RabbitMqEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${irms.rabbitmq.publisher-confirm-timeout-millis:5000}") long confirmTimeoutMillis,
            @Value("${irms.rabbitmq.publisher-return-wait-millis:200}") long returnWaitMillis
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.confirmTimeoutMillis = confirmTimeoutMillis;
        this.returnWaitMillis = returnWaitMillis;
        this.rabbitTemplate.setMandatory(true);
        this.rabbitTemplate.setReturnsCallback(returned -> {
            String messageId = returned.getMessage().getMessageProperties().getMessageId();
            if (messageId == null) {
                return;
            }
            PublishTracker tracker = trackers.get(messageId);
            if (tracker != null) {
                tracker.returned.complete(returned);
            }
        });
    }

    public RabbitMqPublishResult publish(OutboxEvent event) {
        String messageId = event.eventId().toString();
        PublishTracker tracker = new PublishTracker();
        trackers.put(messageId, tracker);
        try {
            CorrelationData correlationData = new CorrelationData(messageId);
            rabbitTemplate.convertAndSend(event.exchangeName(), event.routingKey(), envelope(event), message -> {
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                message.getMessageProperties().setContentType("application/json");
                message.getMessageProperties().setMessageId(messageId);
                message.getMessageProperties().setCorrelationId(event.correlationId());
                message.getMessageProperties().setTimestamp(java.util.Date.from(event.occurredAt()));
                message.getMessageProperties().setHeader(RabbitMqMessageHeaders.EVENT_ID, messageId);
                message.getMessageProperties().setHeader(RabbitMqMessageHeaders.EVENT_TYPE, event.eventType());
                message.getMessageProperties().setHeader(RabbitMqMessageHeaders.EVENT_VERSION, event.eventVersion());
                message.getMessageProperties().setHeader(RabbitMqMessageHeaders.AGGREGATE_TYPE, event.aggregateType());
                message.getMessageProperties().setHeader(RabbitMqMessageHeaders.AGGREGATE_ID, event.aggregateId());
                message.getMessageProperties().setHeader(RabbitMqMessageHeaders.OCCURRED_AT, event.occurredAt().toString());
                message.getMessageProperties().setHeader(RabbitMqMessageHeaders.PRODUCER_SERVICE, event.producerService());
                message.getMessageProperties().setHeader(RabbitMqMessageHeaders.CORRELATION_ID, event.correlationId());
                message.getMessageProperties().setHeader(RabbitMqMessageHeaders.CAUSATION_ID, event.causationId());
                message.getMessageProperties().setHeader(RabbitMqMessageHeaders.IDEMPOTENCY_KEY, event.idempotencyKey());
                return message;
            }, correlationData);

            CorrelationData.Confirm confirm = correlationData.getFuture().get(confirmTimeoutMillis, TimeUnit.MILLISECONDS);
            if (!confirm.isAck()) {
                return RabbitMqPublishResult.nacked(confirm.getReason());
            }
            ReturnedMessage returned = tracker.awaitReturn(returnWaitMillis);
            if (returned != null) {
                return RabbitMqPublishResult.confirmedButReturned(describeReturn(returned));
            }
            return RabbitMqPublishResult.confirmedAndRoutedResult();
        } catch (TimeoutException exception) {
            return RabbitMqPublishResult.timeout("RabbitMQ publisher confirm timed out after " + confirmTimeoutMillis + " ms.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return RabbitMqPublishResult.failed("RabbitMQ publish was interrupted.");
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            return RabbitMqPublishResult.failed(cause.getMessage());
        } catch (RuntimeException exception) {
            return RabbitMqPublishResult.failed(exception.getMessage());
        } finally {
            trackers.remove(messageId);
        }
    }

    private String describeReturn(ReturnedMessage returned) {
        return "Unroutable RabbitMQ publish [replyCode=" + returned.getReplyCode()
                + ", replyText=" + Objects.toString(returned.getReplyText(), "")
                + ", exchange=" + Objects.toString(returned.getExchange(), "")
                + ", routingKey=" + Objects.toString(returned.getRoutingKey(), "") + "]";
    }

    private EventEnvelope envelope(OutboxEvent event) {
        EventMetadata metadata = new EventMetadata(
                event.eventId(),
                event.eventType(),
                event.eventVersion(),
                event.aggregateType(),
                event.aggregateId(),
                event.occurredAt(),
                event.producerService(),
                event.correlationId(),
                event.causationId(),
                event.idempotencyKey()
        );
        return new EventEnvelope(metadata, event.payload());
    }

    private static final class PublishTracker {
        private final CompletableFuture<ReturnedMessage> returned = new CompletableFuture<>();

        private ReturnedMessage awaitReturn(long waitMillis) {
            try {
                return returned.get(waitMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return null;
            } catch (ExecutionException | TimeoutException ignored) {
                return null;
            }
        }
    }
}
