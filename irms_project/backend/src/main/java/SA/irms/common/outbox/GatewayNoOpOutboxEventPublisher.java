package SA.irms.common.outbox;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The API gateway is a stateless edge runtime and should not require the
 * transactional outbox stack just to proxy requests or expose health.
 */
@Component
@Profile("api-gateway")
public class GatewayNoOpOutboxEventPublisher implements OutboxEventPublisher {
    @Override
    public void publish(String type, String aggregateType, String aggregateId, Map<String, Object> payload, String correlationId) {
        // Gateway runtime intentionally does not persist outbox events.
    }
}
