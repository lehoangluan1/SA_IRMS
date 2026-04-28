package SA.irms.inventory.application;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryStockService {
    private final InventoryStockTransactionService transactionService;
    private final InventoryKitchenConsumptionService kitchenConsumptionService;
    private final InventoryLowStockAlertService lowStockAlertService;

    InventoryStockService(
            InventoryStockTransactionService transactionService,
            InventoryKitchenConsumptionService kitchenConsumptionService,
            InventoryLowStockAlertService lowStockAlertService
    ) {
        this.transactionService = transactionService;
        this.kitchenConsumptionService = kitchenConsumptionService;
        this.lowStockAlertService = lowStockAlertService;
    }

    @Transactional
    public void acknowledgeAlert(UUID alertId, UUID actorUserId) {
        lowStockAlertService.acknowledgeAlert(alertId, actorUserId);
    }

    @Transactional
    public void recordManualAdjustment(UUID inventoryItemId, BigDecimal delta, UUID actorUserId, String correlationId) {
        transactionService.recordTransaction(inventoryItemId, delta, "manual_adjustment", actorUserId, correlationId, null, null);
    }

    @Transactional
    public void consumeForKitchenStart(UUID orderItemId, UUID actorUserId, String correlationId) {
        kitchenConsumptionService.consumeForKitchenStart(orderItemId, actorUserId, correlationId);
    }
}
