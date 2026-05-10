package SA.irms.billing.application;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.security.AuthenticatedUser;
import SA.irms.common.context.RequestMetadata;

@Service
public class BillingService {
    private final BillingOverviewService billingOverviewService;
    private final BillingBillCreationService billingBillCreationService;
    private final BillingAdjustmentService billingAdjustmentService;
    private final BillingSplitService billingSplitService;
    private final BillingPaymentService billingPaymentService;
    private final BillingRefundService billingRefundService;
    private final BillingReceiptService billingReceiptService;

    public BillingService(
            BillingOverviewService billingOverviewService,
            BillingBillCreationService billingBillCreationService,
            BillingAdjustmentService billingAdjustmentService,
            BillingSplitService billingSplitService,
            BillingPaymentService billingPaymentService,
            BillingRefundService billingRefundService,
            BillingReceiptService billingReceiptService
    ) {
        this.billingOverviewService = billingOverviewService;
        this.billingBillCreationService = billingBillCreationService;
        this.billingAdjustmentService = billingAdjustmentService;
        this.billingSplitService = billingSplitService;
        this.billingPaymentService = billingPaymentService;
        this.billingRefundService = billingRefundService;
        this.billingReceiptService = billingReceiptService;
    }

    public SA.irms.billing.application.view.BillingViews.BillingOverview load() {
        return load(null, null);
    }

    public SA.irms.billing.application.view.BillingViews.BillingOverview load(UUID billId, UUID tableSessionId) {
        return billingOverviewService.load(billId, tableSessionId);
    }

    @Transactional
    public SA.irms.billing.application.view.BillingViews.BillView createBill(UUID tableSessionId) {
        return billingBillCreationService.createBill(tableSessionId);
    }

    @Transactional
    public SA.irms.billing.application.view.BillingViews.BillView updateBill(
            UUID billId,
            BigDecimal tipAmount,
            BigDecimal discountAmount,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata httpServletRequest
    ) {
        return billingAdjustmentService.updateBill(billId, tipAmount, discountAmount, actor, correlationId, httpServletRequest);
    }

    @Transactional
    public SA.irms.billing.application.view.BillingViews.BillView applyPromotion(UUID billId, String code) {
        return billingAdjustmentService.applyPromotion(billId, code);
    }

    @Transactional
    public SA.irms.billing.application.view.BillingViews.BillView splitBill(UUID billId, SA.irms.billing.application.command.BillingCommands.SplitRequest request) {
        return billingSplitService.splitBill(billId, request);
    }

    @Transactional
    public SA.irms.billing.application.view.BillingViews.PaymentView processPayment(UUID billId, SA.irms.billing.application.command.BillingCommands.PaymentRequest request, AuthenticatedUser actor) {
        return billingPaymentService.processPayment(billId, request, actor);
    }

    @Transactional
    public SA.irms.billing.application.view.BillingViews.ReceiptView issueReceipt(UUID paymentId, SA.irms.billing.application.command.BillingCommands.ReceiptRequest request) {
        return billingReceiptService.issueReceipt(paymentId, request);
    }

    public byte[] buildReceiptDocument(UUID paymentId) {
        return billingReceiptService.buildReceiptDocument(paymentId);
    }

    @Transactional
    public SA.irms.billing.application.view.BillingViews.RefundView refundPayment(
            UUID paymentId,
            SA.irms.billing.application.command.BillingCommands.RefundRequest request,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata httpServletRequest
    ) {
        return billingRefundService.refundPayment(paymentId, request, actor, correlationId, httpServletRequest);
    }

    public List<SA.irms.billing.application.view.BillingViews.RefundView> pendingRefunds() {
        return billingRefundService.pendingRefunds();
    }

    @Transactional
    public SA.irms.billing.application.view.BillingViews.RefundView reviewRefund(
            UUID refundId,
            SA.irms.billing.application.command.BillingCommands.RefundApprovalRequest request,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata httpServletRequest
    ) {
        return billingRefundService.reviewRefund(refundId, request, actor, correlationId, httpServletRequest);
    }

}
