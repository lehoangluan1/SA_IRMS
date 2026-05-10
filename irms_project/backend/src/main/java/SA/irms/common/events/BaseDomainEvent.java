package SA.irms.common.events;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class BaseDomainEvent implements DomainEvent {
    private final String eventType;
    private final int eventVersion;
    private final String aggregateType;
    private final String aggregateId;
    private final Map<String, Object> payload;

    protected BaseDomainEvent(String eventType, int eventVersion, String aggregateType, String aggregateId, Map<String, Object> payload) {
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(payload));
    }

    @Override
    public String eventType() {
        return eventType;
    }

    @Override
    public int eventVersion() {
        return eventVersion;
    }

    @Override
    public String aggregateType() {
        return aggregateType;
    }

    @Override
    public String aggregateId() {
        return aggregateId;
    }

    @Override
    public Map<String, Object> payload() {
        return payload;
    }
}
