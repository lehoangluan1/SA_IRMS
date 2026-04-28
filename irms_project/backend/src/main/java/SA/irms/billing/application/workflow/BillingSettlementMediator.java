package SA.irms.billing.application.workflow;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.workflow.WorkflowExecutionSupport;

@Service
public class BillingSettlementMediator {
    private static final String WORKFLOW_TYPE = "BillingSettlement";

    private final List<BillingSettlementStep> steps;
    private final WorkflowExecutionSupport workflow;

    public BillingSettlementMediator(List<BillingSettlementStep> steps, WorkflowExecutionSupport workflow) {
        this.steps = List.copyOf(steps);
        this.workflow = workflow;
    }

    @Transactional
    public void handlePaymentCompleted(EventEnvelope event) {
        executeSettlement(event, "Payment", "PAYMENT_COMPLETED_RECEIVED");
    }

    @Transactional
    public void handleRefundIssued(EventEnvelope event) {
        executeSettlement(event, "Refund", "REFUND_ISSUED_RECEIVED");
    }

    @Transactional
    public void replaySettlement(String aggregateType, String aggregateId, String reason) {
        workflow.requestReplay(WORKFLOW_TYPE, aggregateType, aggregateId, reason);
    }

    private void executeSettlement(EventEnvelope event, String aggregateType, String initialState) {
        String aggregateId = event.metadata().aggregateId();
        workflow.startOrResume(WORKFLOW_TYPE, aggregateType, aggregateId, initialState, event,
                Map.of("eventType", event.metadata().eventType(), "aggregateId", aggregateId), Set.of("SETTLED", "FAILED"));
        try {
            for (BillingSettlementStep step : steps) {
                if (step.supports(event)) {
                    step.execute(event);
                    workflow.recordStep(WORKFLOW_TYPE, aggregateType, aggregateId, step.name(),
                            SA.irms.common.workflow.WorkflowStepStatus.COMPLETED, event, null, event.payload());
                }
            }
            workflow.transition(WORKFLOW_TYPE, aggregateType, aggregateId, "SETTLED", event,
                    Map.of("eventType", event.metadata().eventType(), "aggregateId", aggregateId));
        } catch (RuntimeException exception) {
            workflow.fail(WORKFLOW_TYPE, aggregateType, aggregateId, "SETTLEMENT_STEP", event, exception, event.payload());
            throw exception;
        }
    }
}
