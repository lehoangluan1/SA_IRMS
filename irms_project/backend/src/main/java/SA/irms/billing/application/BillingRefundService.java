package SA.irms.billing.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.security.AuthenticatedUser;
import SA.irms.common.context.RequestMetadata;

@Service
public class BillingRefundService {
    private final BillingRefundRequestService requestService;
    private final BillingRefundReviewService reviewService;
    private final BillingReadService billingReadService;

    BillingRefundService(BillingRefundRequestService requestService, BillingRefundReviewService reviewService,
            BillingReadService billingReadService) {
        this.requestService = requestService;
        this.reviewService = reviewService;
        this.billingReadService = billingReadService;
    }

    @Transactional
    public SA.irms.billing.application.view.BillingViews.RefundView refundPayment(UUID paymentId, SA.irms.billing.application.command.BillingCommands.RefundRequest request, AuthenticatedUser actor,
            String correlationId, RequestMetadata httpServletRequest) {
        return requestService.refundPayment(paymentId, request, actor, correlationId, httpServletRequest);
    }

    public List<SA.irms.billing.application.view.BillingViews.RefundView> pendingRefunds() {
        return billingReadService.pendingRefunds();
    }

    @Transactional
    public SA.irms.billing.application.view.BillingViews.RefundView reviewRefund(UUID refundId, SA.irms.billing.application.command.BillingCommands.RefundApprovalRequest request, AuthenticatedUser actor,
            String correlationId, RequestMetadata httpServletRequest) {
        return reviewService.reviewRefund(refundId, request, actor, correlationId, httpServletRequest);
    }
}
