package SA.irms.billing.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentRecord(
        UUID paymentId,
        UUID billId,
        UUID splitId,
        String splitLabel,
        String method,
        BigDecimal amount,
        String status,
        String gatewayReference,
        Instant paidAt
) {
}
