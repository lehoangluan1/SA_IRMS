package SA.irms.inventory.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class UsageBasedReorderRecommendationStrategy implements ReorderRecommendationStrategy {
    @Override
    public ReorderRecommendation compute(StockLevel stockLevel) {
        BigDecimal safeThreshold = stockLevel.threshold();
        BigDecimal safeTarget = stockLevel.targetQty();
        BigDecimal safeOnHand = stockLevel.onHand();
        BigDecimal averageUsage = safeThreshold.max(BigDecimal.ONE).divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
        BigDecimal projectedNeed = averageUsage.multiply(BigDecimal.valueOf(Math.max(1, stockLevel.leadTimeDays())));
        BigDecimal reorderQuantity = safeTarget.max(projectedNeed).subtract(safeOnHand).max(BigDecimal.ZERO);
        String confidence = safeOnHand.compareTo(safeThreshold) <= 0 ? "calculated" : "monitor";
        return new ReorderRecommendation(
                stockLevel.inventoryItemId(),
                stockLevel.name(),
                safeThreshold,
                safeTarget,
                safeOnHand,
                averageUsage,
                projectedNeed,
                reorderQuantity,
                confidence
        );
    }
}
