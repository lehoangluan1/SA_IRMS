package SA.irms.billing.infrastructure.payment;

import SA.irms.billing.application.PaymentGateway;
import SA.irms.billing.application.view.PaymentRecord;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ExternalReferencePaymentGateway implements PaymentGateway {
    private static final List<String> SUPPORTED_METHODS = List.of("credit_card", "debit_card", "mobile_payment", "gift_card");

    @Override
    public boolean supports(String method) {
        return SUPPORTED_METHODS.contains(method);
    }

    @Override
    public PaymentAuthorizationResult authorize(String method, BigDecimal amount, BigDecimal amountReceived, UUID paymentId) {
        return new PaymentAuthorizationResult("completed", "txn-" + paymentId, null, BigDecimal.ZERO);
    }

    @Override
    public RefundResult refund(PaymentRecord payment, BigDecimal amount, UUID refundId) {
        if (payment.gatewayReference() == null || payment.gatewayReference().isBlank()) {
            return new RefundResult("pending_review", null, "Gateway reference is unavailable for automated refund.");
        }
        return new RefundResult("completed", "refund-" + refundId, null);
    }
}
