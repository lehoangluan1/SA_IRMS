package SA.irms.inventory.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface InventoryLowStockAlertRepository {
    Optional<LowStockSnapshot> findSnapshot(UUID inventoryItemId);

    boolean hasOpenAlert(UUID inventoryItemId);

    UUID createOpenAlert(UUID inventoryItemId, String severity);

    void acknowledgeAlert(UUID alertId, UUID actorUserId);

    record LowStockSnapshot(UUID inventoryItemId, String name, BigDecimal current, BigDecimal minimum) {
    }
    void resolveOpenAlerts(UUID inventoryItemId);
}