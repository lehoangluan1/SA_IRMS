package SA.irms.common.outbox;

import java.util.Map;

public interface OutboxEventPublisher {
    void publish(String type, String aggregateType, String aggregateId, Map<String, Object> payload, String correlationId);
}
