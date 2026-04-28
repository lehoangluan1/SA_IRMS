package SA.irms.inventory.application.view;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class InventoryViews {
    private InventoryViews() {}

    public record InventoryOverview(List<IngredientView> items, List<TransactionView> transactions,
                                    List<ReorderRecommendationView> recommendations, List<AlertView> alerts) {}
    public record IngredientView(UUID id, String name, String unit, BigDecimal current, BigDecimal min, BigDecimal max,
                                 BigDecimal cost, String category, String lastRestock, List<String> affected) {}
    public record TransactionView(UUID id, String item, String type, BigDecimal quantity, String by, String time) {}
    public record ReorderRecommendationView(UUID inventoryItemId, String name, BigDecimal threshold, BigDecimal targetQty,
                                            BigDecimal onHand, BigDecimal averageUsage, BigDecimal projectedNeed,
                                            BigDecimal recommendedOrderQty, String confidence) {}
    public record AlertView(UUID alertId, UUID inventoryItemId, String itemName, String severity, String status,
                            String createdAt, String acknowledgedAt, String acknowledgedBy, List<String> affectedDishes) {}
}
