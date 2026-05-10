package SA.irms.billing.application.view;

import java.math.BigDecimal;
import java.util.UUID;

public final class BillingReadModels {
    private BillingReadModels() {}

    public record BillingRefundRecord(UUID id, UUID paymentId, UUID billId, BigDecimal amount, String reason, String status, UUID requestedBy) {}
}
