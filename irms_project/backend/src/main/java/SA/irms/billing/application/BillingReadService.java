package SA.irms.billing.application;

import SA.irms.billing.application.view.PaymentRecord;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import SA.irms.billing.application.port.out.BillQueryRepository;
import SA.irms.billing.application.port.out.PaymentQueryRepository;
import SA.irms.billing.application.port.out.RefundQueryRepository;

@Service
public class BillingReadService {
    private final BillQueryRepository billReadService;
    private final PaymentQueryRepository paymentReadService;
    private final RefundQueryRepository refundReadService;

    BillingReadService(BillQueryRepository billReadService, PaymentQueryRepository paymentReadService,
            RefundQueryRepository refundReadService) {
        this.billReadService = billReadService;
        this.paymentReadService = paymentReadService;
        this.refundReadService = refundReadService;
    }

    public SA.irms.billing.application.view.BillingViews.BillView findBill(UUID billId) { return billReadService.findBill(billId); }
    public UUID resolveCurrentBillId(UUID billId, UUID tableSessionId) { return billReadService.resolveCurrentBillId(billId, tableSessionId); }
    public SA.irms.billing.application.view.BillingViews.PaymentView findPayment(UUID paymentId) { return paymentReadService.findPayment(paymentId); }
    public SA.irms.billing.application.view.BillingViews.ReceiptView findReceipt(UUID paymentId) { return paymentReadService.findReceipt(paymentId); }
    public List<SA.irms.billing.application.view.BillingViews.PaymentView> loadPayments(UUID billId) { return paymentReadService.loadPayments(billId); }
    public PaymentRecord loadPaymentRecord(UUID paymentId) { return paymentReadService.loadPaymentRecord(paymentId); }
    public SA.irms.billing.application.view.BillingReadModels.BillingRefundRecord loadRefundRecord(UUID refundId) { return refundReadService.loadRefundRecord(refundId); }
    public SA.irms.billing.application.view.BillingViews.RefundView findRefund(UUID refundId) { return refundReadService.findRefund(refundId); }
    public List<SA.irms.billing.application.view.BillingViews.RefundView> pendingRefunds() { return refundReadService.pendingRefunds(); }

}
