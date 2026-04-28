package SA.irms.billing.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillIssueRepository {
    Optional<UUID> findExistingBillId(UUID tableSessionId);

    long countUnreadyItems(UUID tableSessionId);

    List<BillLineSource> loadBillLineSources(UUID tableSessionId);

    void insertBill(UUID billId, UUID tableSessionId, BigDecimal subTotal, BigDecimal taxAmount,
                    BigDecimal serviceFee, BigDecimal grandTotal, String correlationId);

    void insertBillLine(UUID billId, UUID sourceOrderItemId, String label, int quantity, BigDecimal lineTotal);

    void insertFullBillSplit(UUID billId, BigDecimal allocatedTotal);

    void updateTableSessionToBilling(UUID tableSessionId);

    record BillLineSource(UUID orderItemId, String name, int quantity, BigDecimal unitPrice,
                          BigDecimal modifierTotal, String modifiers) {
    }
}
