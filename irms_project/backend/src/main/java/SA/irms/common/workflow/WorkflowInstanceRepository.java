package SA.irms.common.workflow;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowInstanceRepository {
    boolean start(String workflowType, String aggregateType, String aggregateId, String initialState,
                  String correlationId, UUID eventId, Map<String, Object> payload);

    Optional<WorkflowInstance> find(String workflowType, String aggregateType, String aggregateId);

    void transition(String workflowType, String aggregateType, String aggregateId, String state,
                    UUID eventId, Map<String, Object> payload, String failureReason);

    void markReplayRequested(String workflowType, String aggregateType, String aggregateId, String reason);
}
