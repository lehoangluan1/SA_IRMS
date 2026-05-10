package SA.irms.common.workflow;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import SA.irms.common.events.EventEnvelope;

@Service
public class WorkflowExecutionSupport {
    private final WorkflowInstanceRepository repository;
    private final WorkflowStepRecorder stepRecorder;
    private final WorkflowFailureClassifier failureClassifier;

    public WorkflowExecutionSupport(
            WorkflowInstanceRepository repository,
            WorkflowStepRecorder stepRecorder,
            WorkflowFailureClassifier failureClassifier
    ) {
        this.repository = repository;
        this.stepRecorder = stepRecorder;
        this.failureClassifier = failureClassifier;
    }

    public boolean startOrResume(String workflowType, String aggregateType, String aggregateId,
                                 String initialState, EventEnvelope event,
                                 Map<String, Object> payload, Set<String> terminalStates) {
        boolean started = repository.start(workflowType, aggregateType, aggregateId, initialState,
                event.metadata().correlationId(), event.metadata().eventId(), payload);
        if (started) {
            recordStep(workflowType, aggregateType, aggregateId, initialState, WorkflowStepStatus.STARTED, event, null, payload);
            return true;
        }
        Optional<WorkflowInstance> instance = repository.find(workflowType, aggregateType, aggregateId);
        return instance.map(value -> !terminalStates.contains(value.state())).orElse(false);
    }

    public Optional<WorkflowInstance> find(String workflowType, String aggregateType, String aggregateId) {
        return repository.find(workflowType, aggregateType, aggregateId);
    }

    public void transition(String workflowType, String aggregateType, String aggregateId, String state,
                           EventEnvelope event, Map<String, Object> payload) {
        repository.transition(workflowType, aggregateType, aggregateId, state,
                event.metadata().eventId(), payload, null);
        recordStep(workflowType, aggregateType, aggregateId, state, WorkflowStepStatus.COMPLETED, event, null, payload);
    }

    public void fail(String workflowType, String aggregateType, String aggregateId, String stepName,
                     EventEnvelope event, Throwable failure, Map<String, Object> payload) {
        WorkflowFailureClassifier.FailureType failureType = failureClassifier.classify(failure);
        String state = failureType == WorkflowFailureClassifier.FailureType.RETRYABLE ? "RETRYABLE_FAILED" : "FAILED";
        String reason = failure == null || failure.getMessage() == null ? "Workflow failed." : failure.getMessage();
        repository.transition(workflowType, aggregateType, aggregateId, state,
                event.metadata().eventId(), payload, reason);
        recordStep(workflowType, aggregateType, aggregateId, stepName, WorkflowStepStatus.FAILED, event, reason, payload);
    }

    public void requestReplay(String workflowType, String aggregateType, String aggregateId, String reason) {
        repository.markReplayRequested(workflowType, aggregateType, aggregateId, reason);
    }

    public void recordStep(String workflowType, String aggregateType, String aggregateId, String stepName,
                           WorkflowStepStatus status, EventEnvelope event, String error, Map<String, Object> payload) {
        UUID eventId = event == null ? null : event.metadata().eventId();
        stepRecorder.record(workflowType, aggregateType, aggregateId, stepName, status, eventId, error, payload);
    }
}
