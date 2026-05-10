package SA.irms.common.outbox;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import SA.irms.common.events.DomainEvent;
import SA.irms.common.events.EventMetadata;
import SA.irms.common.events.EventRouting;
import SA.irms.common.events.EventRoutingRegistry;

@Service
public class TransactionalOutboxPublisher implements DomainEventPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String defaultProducerService;

    public TransactionalOutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${spring.application.name:irms-service}") String defaultProducerService
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper.copy().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        this.clock = clock;
        this.defaultProducerService = defaultProducerService;
    }

    @Transactional
    public UUID publish(DomainEvent event, String correlationId, String causationId) {
        String idempotencyKey = idempotencyKey(event.eventType(), event.aggregateType(), event.aggregateId(), correlationId, event.payload());
        EventMetadata metadata = EventMetadata.create(event, defaultProducerService, correlationId, causationId, idempotencyKey);
        return publish(metadata, event.payload());
    }

    @Transactional
    public UUID publish(String eventType, int eventVersion, String aggregateType, String aggregateId,
                        Map<String, Object> payload, String correlationId, String causationId, String producerService) {
        String normalizedProducer = producerService == null || producerService.isBlank() ? defaultProducerService : producerService;
        String idempotencyKey = idempotencyKey(eventType, aggregateType, aggregateId, correlationId, payload);
        EventMetadata metadata = new EventMetadata(UUID.randomUUID(), eventType, eventVersion, aggregateType, aggregateId,
                Instant.now(clock), normalizedProducer, correlationId, causationId, idempotencyKey);
        return publish(metadata, payload);
    }

    @Transactional
    public UUID publish(EventMetadata metadata, Map<String, Object> payload) {
        EventRouting routing = EventRoutingRegistry.routingFor(metadata.eventType());
        return outboxEventRepository.storePending(metadata, payload, routing);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Outbox payload could not be serialized.", exception);
        }
    }

    private String idempotencyKey(String eventType, String aggregateType, String aggregateId, String correlationId, Map<String, Object> payload) {
        String material = eventType + "|" + aggregateType + "|" + aggregateId + "|" + (correlationId == null ? "" : correlationId) + "|" + toJson(payload);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable.", exception);
        }
    }
}
