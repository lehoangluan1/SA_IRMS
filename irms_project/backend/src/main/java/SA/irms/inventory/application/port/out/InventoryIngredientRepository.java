package SA.irms.inventory.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface InventoryIngredientRepository {
    void createIngredient(UUID inventoryItemId, UUID branchId, String name, String unit, BigDecimal onHand,
                          BigDecimal minimumStock, BigDecimal maximumStock, BigDecimal costPerUnit, String category);

    void updateIngredient(UUID inventoryItemId, String name, String unit, BigDecimal onHand,
                          BigDecimal minimumStock, BigDecimal maximumStock, BigDecimal costPerUnit, String category);

    void upsertReorderRule(UUID inventoryItemId, BigDecimal threshold, BigDecimal targetQty);

    long countDependentRecipes(UUID inventoryItemId);

    int deleteIngredient(UUID inventoryItemId);
}
