package SA.irms.common.messaging;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import SA.irms.common.events.EventEnvelope;

@Component
public class EventMessageConverter {
    private final ObjectMapper objectMapper;

    public EventMessageConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EventEnvelope fromMessage(Message message) {
        try {
            return objectMapper.readValue(message.getBody(), EventEnvelope.class);
        } catch (IOException exception) {
            throw new IllegalArgumentException("RabbitMQ event message could not be decoded.", exception);
        }
    }
}
