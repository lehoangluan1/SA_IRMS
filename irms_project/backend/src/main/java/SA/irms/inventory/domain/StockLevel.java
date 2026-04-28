package SA.irms.inventory.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record StockLevel(
        UUID inventoryItemId,
        String name,
        BigDecimal threshold,
        BigDecimal targetQty,
        int leadTimeDays,
        BigDecimal onHand
) {
    public StockLevel {
        threshold = threshold == null ? BigDecimal.ZERO : threshold;
        targetQty = targetQty == null ? threshold.multiply(BigDecimal.valueOf(2)) : targetQty;
        onHand = onHand == null ? BigDecimal.ZERO : onHand;
    }
}
