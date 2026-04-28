package SA.irms.billing.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface BillAdjustmentRepository {
    void updateBillTotals(UUID billId, BigDecimal tipAmount, BigDecimal discountAmount);

    void syncSingleSplit(UUID billId, BigDecimal tipAmount);
}
