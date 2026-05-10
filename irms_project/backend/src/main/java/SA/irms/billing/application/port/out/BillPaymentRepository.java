package SA.irms.billing.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface BillPaymentRepository {
    BigDecimal calculateOutstanding(UUID billId, UUID splitId);

    void recordPayment(UUID paymentId, UUID billId, UUID splitId, String method, BigDecimal amount,
                       String status, String gatewayRef, BigDecimal cashReceived, BigDecimal changeDue,
                       UUID processedByUserId);

    void markSplitPaid(UUID splitId);

    void updateBillSettlementStatus(UUID billId, String status, boolean closeBill);
}
