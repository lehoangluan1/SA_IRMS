package SA.irms.common.messaging;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import SA.irms.common.events.EventEnvelope;

@Service
public class DeadLetterEventService {
    private final DeadLetterEventRepository deadLetterEventRepository;
    private final ObjectMapper objectMapper;

    public DeadLetterEventService(DeadLetterEventRepository deadLetterEventRepository, ObjectMapper objectMapper) {
        this.deadLetterEventRepository = deadLetterEventRepository;
        this.objectMapper = objectMapper;
    }

    public void record(EventEnvelope envelope, String consumerName, String failureReason, Map<String, Object> brokerHeaders) {
        deadLetterEventRepository.record(new DeadLetterEventRepository.DeadLetterEventRecord(
                UUID.randomUUID(),
                envelope.metadata().eventId(),
                consumerName,
                envelope.metadata().eventType(),
                toJson(envelope.payload()),
                toJson(brokerHeaders == null ? Map.of() : brokerHeaders),
                truncate(failureReason),
                envelope.metadata().correlationId(),
                envelope.metadata().causationId()
        ));
    }

    public void recordRaw(String consumerName, String failureReason, Map<String, Object> brokerHeaders, byte[] rawBody) {
        deadLetterEventRepository.record(new DeadLetterEventRepository.DeadLetterEventRecord(
                UUID.randomUUID(),
                null,
                consumerName,
                "UnparseableRabbitMqMessage",
                toJson(Map.of("rawBody", rawBody == null ? "" : new String(rawBody, StandardCharsets.UTF_8))),
                toJson(brokerHeaders == null ? Map.of() : brokerHeaders),
                truncate(failureReason),
                brokerHeaders == null ? null : stringHeader(brokerHeaders, "correlationId"),
                brokerHeaders == null ? null : stringHeader(brokerHeaders, RabbitMqMessageHeaders.CAUSATION_ID)
        ));
    }

    private String stringHeader(Map<String, Object> headers, String key) {
        Object value = headers.get(key);
        return value == null ? null : value.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize dead-letter payload.", exception);
        }
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown DLQ failure.";
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
