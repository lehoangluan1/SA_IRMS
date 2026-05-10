package SA.irms.common.workflow;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record WorkflowInstance(
        UUID workflowId,
        String workflowType,
        String aggregateType,
        String aggregateId,
        String state,
        int version,
        String correlationId,
        String failureReason,
        UUID lastEventId,
        Map<String, Object> payload,
        Instant createdAt,
        Instant updatedAt
) {
    public WorkflowInstance {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
