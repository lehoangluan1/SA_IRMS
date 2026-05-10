package SA.irms.billing.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class BillingViews {
    private BillingViews() {}

    public record BillingOverview(
            BillView currentBill,
            List<BillableSessionView> billableSessions,
            List<RecentBillView> recentBills,
            List<RefundView> refundQueue
    ) {}

    public record BillView(
            UUID id,
            UUID tableSessionId,
            int tableNumber,
            List<LineView> items,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal taxRate,
            BigDecimal serviceFee,
            BigDecimal discount,
            BigDecimal tipAmount,
            BigDecimal total,
            String status,
            List<SplitView> splits,
            List<PaymentView> payments
    ) {}

    public record LineView(UUID lineId, String name, int quantity, BigDecimal total, String modifiers) {}
    public record SplitView(UUID id, String label, BigDecimal amount, BigDecimal tipAmount, String status) {}
    public record RecentBillView(UUID id, int tableNumber, BigDecimal total, String status, String method, String time) {}
    public record BillableSessionView(UUID sessionId, int tableNumber, int guests, String openedAt) {}

    public record PaymentView(
            UUID id,
            UUID billId,
            UUID splitId,
            String splitLabel,
            String method,
            BigDecimal amount,
            String status,
            String paidAt
    ) {}

    public record ReceiptView(UUID id, UUID billId, String channel, Instant issuedAt, String recipientAddress) {}

    public record RefundView(
            UUID id,
            UUID paymentId,
            UUID billId,
            BigDecimal amount,
            String reason,
            String status,
            String requestedBy,
            String approvedBy,
            String paymentMethod,
            int tableNumber,
            String createdAt
    ) {}
}
