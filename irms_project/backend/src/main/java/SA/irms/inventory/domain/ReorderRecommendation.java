package SA.irms.inventory.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record ReorderRecommendation(
        UUID inventoryItemId,
        String name,
        BigDecimal threshold,
        BigDecimal targetQty,
        BigDecimal onHand,
        BigDecimal averageUsage,
        BigDecimal projectedNeed,
        BigDecimal recommendedOrderQty,
        String confidence
) {
}
