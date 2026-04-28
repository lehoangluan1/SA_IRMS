package SA.irms.billing.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface BillSplitRepository {
    long countPaidSplits(UUID billId);

    void clearSplits(UUID billId);

    int loadGuestCount(UUID billId);

    void insertSplit(UUID splitId, UUID billId, String label, BigDecimal allocatedTotal, BigDecimal tipAmount);

    void insertSplitAllocation(UUID splitId, UUID billLineId, BigDecimal amount, BigDecimal ratio);
}
