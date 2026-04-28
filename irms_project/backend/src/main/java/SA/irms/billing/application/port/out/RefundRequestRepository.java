package SA.irms.billing.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface RefundRequestRepository {
    void createPendingRefund(UUID refundId, UUID paymentId, UUID billId, BigDecimal amount, String reason, UUID requestedBy);
}
