package SA.irms.billing.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

public interface BillingOverviewQueryRepository {
    List<BillableSessionSummary> loadBillableSessions();

    List<RecentBillSummary> loadRecentBills();

    record BillableSessionSummary(UUID sessionId, int tableNumber, int guests, Instant openedAt) {
    }

    record RecentBillSummary(UUID billId, int tableNumber, java.math.BigDecimal total, String status,
                             String paymentMethod, Instant activityAt) {
    }
}
