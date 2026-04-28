package SA.irms.billing.application;

import SA.irms.billing.application.port.out.BillingOverviewQueryRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BillingOverviewService {
    private final BillingOverviewQueryRepository repository;
    private final BillingReadService billingReadService;
    private final BillingRefundService billingRefundService;
    private final Clock clock;

    public BillingOverviewService(BillingOverviewQueryRepository repository,
                                  BillingReadService billingReadService,
                                  BillingRefundService billingRefundService,
                                  Clock clock) {
        this.repository = repository;
        this.billingReadService = billingReadService;
        this.billingRefundService = billingRefundService;
        this.clock = clock;
    }

    public SA.irms.billing.application.view.BillingViews.BillingOverview load(UUID billId, UUID tableSessionId) {
        UUID selectedBillId = billingReadService.resolveCurrentBillId(billId, tableSessionId);
        SA.irms.billing.application.view.BillingViews.BillView currentBill = selectedBillId == null ? null : billingReadService.findBill(selectedBillId);
        List<SA.irms.billing.application.view.BillingViews.BillableSessionView> billableSessions = repository.loadBillableSessions().stream()
                .map(row -> new SA.irms.billing.application.view.BillingViews.BillableSessionView(row.sessionId(), row.tableNumber(), row.guests(), relativeTime(row.openedAt())))
                .toList();
        List<SA.irms.billing.application.view.BillingViews.RecentBillView> recentBills = repository.loadRecentBills().stream()
                .map(row -> new SA.irms.billing.application.view.BillingViews.RecentBillView(row.billId(), row.tableNumber(), row.total(), row.status(), row.paymentMethod(), relativeTime(row.activityAt())))
                .toList();
        List<SA.irms.billing.application.view.BillingViews.RefundView> refundQueue = billingRefundService.pendingRefunds();
        return new SA.irms.billing.application.view.BillingViews.BillingOverview(currentBill, billableSessions, recentBills, refundQueue);
    }

    private String relativeTime(Instant instant) {
        long minutes = Math.max(0, java.time.Duration.between(instant, Instant.now(clock)).toMinutes());
        if (minutes < 60) {
            return minutes + " min ago";
        }
        return (minutes / 60) + " hr ago";
    }
}
