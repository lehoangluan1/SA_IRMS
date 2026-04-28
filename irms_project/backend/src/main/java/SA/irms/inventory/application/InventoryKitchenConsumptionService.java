package SA.irms.inventory.application;

import SA.irms.inventory.application.port.out.InventoryKitchenConsumptionRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryKitchenConsumptionService {
    private final InventoryKitchenConsumptionRepository repository;
    private final InventoryStockTransactionService transactionService;

    public InventoryKitchenConsumptionService(InventoryKitchenConsumptionRepository repository,
                                              InventoryStockTransactionService transactionService) {
        this.repository = repository;
        this.transactionService = transactionService;
    }

    @Transactional
    public void consumeForKitchenStart(UUID orderItemId, UUID actorUserId, String correlationId) {
        for (InventoryKitchenConsumptionRepository.RecipeUsage usage : repository.loadRecipeUsages(orderItemId)) {
            if (repository.hasRecordedKitchenStartConsumption(usage.inventoryItemId(), orderItemId)) {
                continue;
            }
            BigDecimal totalUsage = usage.requiredQty().add(usage.requiredQty().multiply(usage.wasteFactor()));
            transactionService.recordTransaction(
                    usage.inventoryItemId(),
                    totalUsage.negate(),
                    "consumption",
                    actorUserId,
                    correlationId,
                    orderItemId,
                    "order_item_cooking_start"
            );
        }
    }
}
