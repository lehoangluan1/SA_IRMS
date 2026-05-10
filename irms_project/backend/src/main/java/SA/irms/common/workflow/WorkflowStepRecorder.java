package SA.irms.common.workflow;

import java.util.Map;
import java.util.UUID;

public interface WorkflowStepRecorder {
    void record(String workflowType, String aggregateType, String aggregateId, String stepName,
                WorkflowStepStatus status, UUID eventId, String error, Map<String, Object> payload);
}
