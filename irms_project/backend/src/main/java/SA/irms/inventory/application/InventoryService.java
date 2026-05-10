package SA.irms.inventory.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.context.RequestMetadata;

@Service
public class InventoryService {
    private final InventoryReadService inventoryReadService;
    private final InventoryIngredientService inventoryIngredientService;
    private final InventoryStockService inventoryStockService;

    public InventoryService(InventoryReadService inventoryReadService, InventoryIngredientService inventoryIngredientService,
            InventoryStockService inventoryStockService) {
        this.inventoryReadService = inventoryReadService;
        this.inventoryIngredientService = inventoryIngredientService;
        this.inventoryStockService = inventoryStockService;
    }

    public SA.irms.inventory.application.view.InventoryViews.InventoryOverview load() { return inventoryReadService.load(); }
    public SA.irms.inventory.application.view.InventoryViews.IngredientView createIngredient(SA.irms.inventory.application.command.InventoryCommands.InventoryUpsert request) { return inventoryIngredientService.createIngredient(request); }
    public SA.irms.inventory.application.view.InventoryViews.IngredientView updateIngredient(UUID inventoryItemId, SA.irms.inventory.application.command.InventoryCommands.InventoryUpsert request, UUID actorUserId,
            String correlationId, RequestMetadata httpServletRequest) {
        return inventoryIngredientService.updateIngredient(inventoryItemId, request, actorUserId, correlationId, httpServletRequest);
    }
    public void deleteIngredient(UUID inventoryItemId) { inventoryIngredientService.deleteIngredient(inventoryItemId); }
    @Transactional public void acknowledgeAlert(UUID alertId, UUID actorUserId) { inventoryStockService.acknowledgeAlert(alertId, actorUserId); }
    @Transactional public void consumeForKitchenStart(UUID orderItemId, UUID actorUserId, String correlationId) { inventoryStockService.consumeForKitchenStart(orderItemId, actorUserId, correlationId); }

}
