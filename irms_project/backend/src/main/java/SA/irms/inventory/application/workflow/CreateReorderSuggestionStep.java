package SA.irms.inventory.application.workflow;

import org.springframework.stereotype.Component;

import SA.irms.inventory.application.port.out.ReorderSuggestionCommandPort;

@Component
public class CreateReorderSuggestionStep {
    private final ReorderSuggestionCommandPort reorderSuggestionCommandPort;

    public CreateReorderSuggestionStep(ReorderSuggestionCommandPort reorderSuggestionCommandPort) {
        this.reorderSuggestionCommandPort = reorderSuggestionCommandPort;
    }

    public void execute(EvaluateLowStockStep.Evaluation evaluation) {
        reorderSuggestionCommandPort.upsertSuggestion(
                evaluation.inventoryItemId(),
                evaluation.stockLevel().lowStockThreshold(),
                evaluation.stockLevel().quantityOnHand().add(evaluation.reorderQuantity()),
                3
        );
    }
}
