package SA.irms.billing.application.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class BillingCommands {
    private BillingCommands() {}

    public record SplitRequest(String method, Integer splitCount, List<BigDecimal> amounts, List<BigDecimal> tipAmounts) {}
    public record PaymentRequest(UUID splitId, String method, BigDecimal amount, BigDecimal amountReceived) {}
    public record ReceiptRequest(String channel, String recipientAddress) {}
    public record RefundRequest(BigDecimal amount, String reason) {}
    public record RefundApprovalRequest(String action, String reason) {}
}
