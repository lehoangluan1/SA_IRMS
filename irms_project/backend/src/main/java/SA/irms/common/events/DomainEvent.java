package SA.irms.common.events;

import java.util.Map;

/**
 * Versioned domain-event contract used by independently deployable IRMS services.
 * Implementations must be immutable payload DTOs and must not expose service domain entities.
 */
public interface DomainEvent {
    String eventType();

    default int eventVersion() {
        return 1;
    }

    String aggregateType();

    String aggregateId();

    Map<String, Object> payload();
}
