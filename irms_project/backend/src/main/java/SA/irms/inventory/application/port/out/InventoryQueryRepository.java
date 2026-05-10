package SA.irms.inventory.application.port.out;

import java.util.UUID;

public interface InventoryQueryRepository {
    SA.irms.inventory.application.view.InventoryViews.InventoryOverview load();

    SA.irms.inventory.application.view.InventoryViews.IngredientView findIngredient(UUID inventoryItemId);
}
