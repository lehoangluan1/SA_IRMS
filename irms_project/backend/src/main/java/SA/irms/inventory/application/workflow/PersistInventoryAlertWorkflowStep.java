package SA.irms.inventory.application.workflow;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.workflow.WorkflowExecutionSupport;

@Component
public class PersistInventoryAlertWorkflowStep {
    private final WorkflowExecutionSupport workflow;

    public PersistInventoryAlertWorkflowStep(WorkflowExecutionSupport workflow) {
        this.workflow = workflow;
    }

    public void start(String workflowType, String aggregateType, String aggregateId, EventEnvelope event, Map<String, Object> payload) {
        workflow.startOrResume(workflowType, aggregateType, aggregateId, "STOCK_CHANGED", event, payload,
                java.util.Set.of("REORDER_SUGGESTED", "RESOLVED"));
    }

    public void markDetected(String workflowType, String aggregateType, String aggregateId, EventEnvelope event, Map<String, Object> payload) {
        workflow.transition(workflowType, aggregateType, aggregateId, "LOW_STOCK_DETECTED", event, payload);
    }

    public void markSuggested(String workflowType, String aggregateType, String aggregateId, EventEnvelope event, Map<String, Object> payload) {
        workflow.transition(workflowType, aggregateType, aggregateId, "REORDER_SUGGESTED", event, payload);
    }

    public void fail(String workflowType, String aggregateType, String aggregateId, EventEnvelope event, Throwable failure, Map<String, Object> payload) {
        workflow.fail(workflowType, aggregateType, aggregateId, "INVENTORY_ALERT", event, failure, payload);
    }
}
