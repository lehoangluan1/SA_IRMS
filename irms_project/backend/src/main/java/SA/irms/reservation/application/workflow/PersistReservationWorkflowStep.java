package SA.irms.reservation.application.workflow;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.workflow.WorkflowExecutionSupport;

@Component
public class PersistReservationWorkflowStep {
    private final WorkflowExecutionSupport workflow;

    public PersistReservationWorkflowStep(WorkflowExecutionSupport workflow) {
        this.workflow = workflow;
    }

    public void start(String workflowType, String aggregateType, String aggregateId, String initialState, EventEnvelope event, Map<String, Object> payload) {
        workflow.startOrResume(workflowType, aggregateType, aggregateId, initialState, event, payload, java.util.Set.of("SEATED", "FAILED", "EXPIRED"));
    }

    public void complete(String workflowType, String aggregateType, String aggregateId, String state, EventEnvelope event, Map<String, Object> payload) {
        workflow.transition(workflowType, aggregateType, aggregateId, state, event, payload);
    }

    public void fail(String workflowType, String aggregateType, String aggregateId, EventEnvelope event, Throwable failure, Map<String, Object> payload) {
        workflow.fail(workflowType, aggregateType, aggregateId, "RESERVATION_WORKFLOW", event, failure, payload);
    }
}
