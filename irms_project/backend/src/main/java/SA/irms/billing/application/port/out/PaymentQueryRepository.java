package SA.irms.billing.application.port.out;

import SA.irms.billing.application.PaymentGateway;
import SA.irms.billing.application.view.PaymentRecord;
import java.util.List;
import java.util.UUID;

public interface PaymentQueryRepository {
    SA.irms.billing.application.view.BillingViews.PaymentView findPayment(UUID paymentId);
    SA.irms.billing.application.view.BillingViews.ReceiptView findReceipt(UUID paymentId);
    List<SA.irms.billing.application.view.BillingViews.PaymentView> loadPayments(UUID billId);
    PaymentRecord loadPaymentRecord(UUID paymentId);
}
