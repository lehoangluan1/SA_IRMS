package SA.irms.billing.application;

import java.math.BigDecimal;
import java.util.UUID;

import SA.irms.billing.application.view.PaymentRecord;

public interface PaymentGateway {
    boolean supports(String method);

    PaymentAuthorizationResult authorize(String method, BigDecimal amount, BigDecimal amountReceived, UUID paymentId);

    RefundResult refund(PaymentRecord payment, BigDecimal amount, UUID refundId);

    record PaymentAuthorizationResult(
            String status,
            String gatewayReference,
            BigDecimal cashReceived,
            BigDecimal changeDue
    ) {
    }

    record RefundResult(
            String status,
            String externalReference,
            String failureReason
    ) {
    }
}
