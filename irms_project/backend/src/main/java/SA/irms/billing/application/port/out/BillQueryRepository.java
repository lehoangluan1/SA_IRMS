package SA.irms.billing.application.port.out;

import java.util.UUID;

public interface BillQueryRepository {
    SA.irms.billing.application.view.BillingViews.BillView findBill(UUID billId);
    UUID resolveCurrentBillId(UUID billId, UUID tableSessionId);
}
