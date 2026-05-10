package SA.irms.inventory.application.workflow;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.inventory.application.port.out.InventoryStockLevelQueryPort;
import SA.irms.inventory.application.port.out.InventoryStockLevelQueryPort.StockLevel;
import SA.irms.inventory.domain.ReorderRecommendation;
import SA.irms.inventory.domain.ReorderRecommendationStrategy;

@Component
public class EvaluateLowStockStep {
    private final InventoryStockLevelQueryPort stockLevelQueryPort;
    private final ReorderRecommendationStrategy reorderRecommendationStrategy;

    public EvaluateLowStockStep(InventoryStockLevelQueryPort stockLevelQueryPort,
                                ReorderRecommendationStrategy reorderRecommendationStrategy) {
        this.stockLevelQueryPort = stockLevelQueryPort;
        this.reorderRecommendationStrategy = reorderRecommendationStrategy;
    }

    public Evaluation evaluate(UUID inventoryItemId) {
        StockLevel stockLevel = stockLevelQueryPort.findStockLevel(inventoryItemId).orElse(null);
        if (stockLevel == null || stockLevel.quantityOnHand().compareTo(stockLevel.lowStockThreshold()) > 0) {
            return null;
        }
        BigDecimal targetQty = stockLevel.lowStockThreshold().multiply(BigDecimal.valueOf(2));
        ReorderRecommendation recommendation = reorderRecommendationStrategy.compute(new SA.irms.inventory.domain.StockLevel(
                stockLevel.inventoryItemId(),
                "inventory-item-" + stockLevel.inventoryItemId(),
                stockLevel.lowStockThreshold(),
                targetQty,
                3,
                stockLevel.quantityOnHand()
        ));
        return new Evaluation(
                inventoryItemId,
                stockLevel,
                recommendation.recommendedOrderQty(),
                Map.of(
                        "inventoryItemId", inventoryItemId.toString(),
                        "quantityOnHand", stockLevel.quantityOnHand(),
                        "threshold", stockLevel.lowStockThreshold(),
                        "reorderSuggestion", recommendation.recommendedOrderQty()
                )
        );
    }

    public record Evaluation(UUID inventoryItemId, StockLevel stockLevel, BigDecimal reorderQuantity, Map<String, Object> payload) {
    }
}
