package SA.irms.billing.application.port.out;

import java.util.List;
import java.util.UUID;

public interface RefundQueryRepository {
    SA.irms.billing.application.view.BillingReadModels.BillingRefundRecord loadRefundRecord(UUID refundId);
    SA.irms.billing.application.view.BillingViews.RefundView findRefund(UUID refundId);
    List<SA.irms.billing.application.view.BillingViews.RefundView> pendingRefunds();
}
