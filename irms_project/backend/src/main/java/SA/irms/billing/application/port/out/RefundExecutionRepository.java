package SA.irms.billing.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface RefundExecutionRepository {
    BigDecimal calculateRefundableBalance(UUID paymentId, BigDecimal paymentAmount, UUID excludedRefundId);

    void insertRefund(UUID refundId, UUID paymentId, UUID billId, BigDecimal amount, String reason,
                      String status, UUID requestedBy, UUID approvedBy, String externalTransactionRef);

    void updateQueuedRefund(UUID refundId, String status, UUID approvedBy, String externalTransactionRef);

    void markPaymentRefundedIfFullyRefunded(UUID paymentId, BigDecimal amount);

    void updateBillSettlementStatus(UUID billId, String status, boolean closeBill);
}
