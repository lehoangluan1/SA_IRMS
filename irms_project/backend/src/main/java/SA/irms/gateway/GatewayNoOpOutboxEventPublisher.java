package SA.irms.gateway;

import java.util.Map;

import org.springframework.stereotype.Component;

import SA.irms.common.outbox.OutboxEventPublisher;

@Component
class GatewayNoOpOutboxEventPublisher implements OutboxEventPublisher {
    @Override
    public void publish(String type, String aggregateType, String aggregateId, Map<String, Object> payload, String correlationId) {
        // Gateway runtime is stateless and must not require JDBC/outbox infrastructure.
    }
}
