package SA.irms.common.events;

import java.util.Map;

public record EventEnvelope(
        EventMetadata metadata,
        Map<String, Object> payload
) {
    public EventEnvelope {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata is required.");
        }
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public static EventEnvelope from(DomainEvent event, EventMetadata metadata) {
        return new EventEnvelope(metadata, event.payload());
    }
}
