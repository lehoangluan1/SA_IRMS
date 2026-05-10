package SA.irms.inventory.application.workflow;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.events.EventEnvelope;

@Service
public class InventoryAlertMediator {
    private static final String WORKFLOW_TYPE = "InventoryAlert";
    private static final String AGGREGATE_TYPE = "InventoryItem";

    private final EvaluateLowStockStep evaluateLowStockStep;
    private final CreateReorderSuggestionStep createReorderSuggestionStep;
    private final PublishLowStockDetectedStep publishLowStockDetectedStep;
    private final RequestManagerNotificationStep requestManagerNotificationStep;
    private final PersistInventoryAlertWorkflowStep persistWorkflowStep;

    public InventoryAlertMediator(
            EvaluateLowStockStep evaluateLowStockStep,
            CreateReorderSuggestionStep createReorderSuggestionStep,
            PublishLowStockDetectedStep publishLowStockDetectedStep,
            RequestManagerNotificationStep requestManagerNotificationStep,
            PersistInventoryAlertWorkflowStep persistWorkflowStep
    ) {
        this.evaluateLowStockStep = evaluateLowStockStep;
        this.createReorderSuggestionStep = createReorderSuggestionStep;
        this.publishLowStockDetectedStep = publishLowStockDetectedStep;
        this.requestManagerNotificationStep = requestManagerNotificationStep;
        this.persistWorkflowStep = persistWorkflowStep;
    }

    @Transactional
    public void handleStockChanged(EventEnvelope event) {
        UUID inventoryItemId = UUID.fromString(event.metadata().aggregateId());
        EvaluateLowStockStep.Evaluation evaluation = evaluateLowStockStep.evaluate(inventoryItemId);
        if (evaluation == null) {
            return;
        }
        persistWorkflowStep.start(WORKFLOW_TYPE, AGGREGATE_TYPE, inventoryItemId.toString(), event, evaluation.payload());
        try {
            publishLowStockDetectedStep.execute(event, evaluation);
            requestManagerNotificationStep.execute(event, evaluation);
            persistWorkflowStep.markDetected(WORKFLOW_TYPE, AGGREGATE_TYPE, inventoryItemId.toString(), event, evaluation.payload());
        } catch (RuntimeException exception) {
            persistWorkflowStep.fail(WORKFLOW_TYPE, AGGREGATE_TYPE, inventoryItemId.toString(), event, exception, evaluation.payload());
            throw exception;
        }
    }

    @Transactional
    public void handleReorderSuggestion(EventEnvelope event) {
        UUID inventoryItemId = UUID.fromString(event.metadata().aggregateId());
        EvaluateLowStockStep.Evaluation evaluation = evaluateLowStockStep.evaluate(inventoryItemId);
        if (evaluation == null) {
            return;
        }
        persistWorkflowStep.start(WORKFLOW_TYPE, AGGREGATE_TYPE, inventoryItemId.toString(), event, evaluation.payload());
        try {
            createReorderSuggestionStep.execute(evaluation);
            persistWorkflowStep.markSuggested(WORKFLOW_TYPE, AGGREGATE_TYPE, inventoryItemId.toString(), event, Map.of(
                    "inventoryItemId", inventoryItemId.toString(),
                    "suggestedQuantity", evaluation.reorderQuantity()
            ));
        } catch (RuntimeException exception) {
            persistWorkflowStep.fail(WORKFLOW_TYPE, AGGREGATE_TYPE, inventoryItemId.toString(), event, exception, evaluation.payload());
            throw exception;
        }
    }
}
