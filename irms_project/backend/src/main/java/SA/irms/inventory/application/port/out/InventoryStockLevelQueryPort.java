package SA.irms.inventory.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface InventoryStockLevelQueryPort {
    Optional<StockLevel> findStockLevel(UUID inventoryItemId);

    record StockLevel(UUID inventoryItemId, BigDecimal quantityOnHand, BigDecimal lowStockThreshold) {
    }
}
