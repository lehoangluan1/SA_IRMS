package SA.irms.inventory.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface InventoryStockTransactionRepository {
    BigDecimal lockCurrentOnHand(UUID inventoryItemId);

    boolean hasRecordedSource(UUID inventoryItemId, String reason, UUID sourceRef, String sourceType);

    void updateOnHand(UUID inventoryItemId, BigDecimal newOnHand);

    void insertTransaction(UUID transactionId, UUID inventoryItemId, BigDecimal delta, String reason,
                           UUID sourceRef, String sourceType, BigDecimal previousOnHand, BigDecimal newOnHand,
                           UUID performedByUserId, String correlationId, Instant occurredAt);
}
