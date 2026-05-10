package SA.irms.inventory.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface InventoryKitchenConsumptionRepository {
    List<RecipeUsage> loadRecipeUsages(UUID orderItemId);

    boolean hasRecordedKitchenStartConsumption(UUID inventoryItemId, UUID orderItemId);

    record RecipeUsage(UUID inventoryItemId, BigDecimal requiredQty, BigDecimal wasteFactor) {
    }
}
