package SA.irms.billing.infrastructure.payment;

import SA.irms.billing.application.PaymentGateway;
import SA.irms.billing.application.view.PaymentRecord;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CashPaymentGateway implements PaymentGateway {
    @Override
    public boolean supports(String method) {
        return "cash".equals(method);
    }

    @Override
    public PaymentAuthorizationResult authorize(String method, BigDecimal amount, BigDecimal amountReceived, UUID paymentId) {
        BigDecimal received = amountReceived == null ? amount : amountReceived;
        BigDecimal changeDue = received.subtract(amount);
        return new PaymentAuthorizationResult("completed", null, received, changeDue.max(BigDecimal.ZERO));
    }

    @Override
    public RefundResult refund(PaymentRecord payment, BigDecimal amount, UUID refundId) {
        return new RefundResult("completed", null, null);
    }
}
