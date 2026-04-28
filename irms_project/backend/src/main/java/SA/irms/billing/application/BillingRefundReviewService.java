package SA.irms.billing.application;

import SA.irms.billing.application.view.PaymentRecord;
import SA.irms.billing.application.port.out.RefundReviewRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import SA.irms.common.error.ConflictException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.identity.SharedIdentityPolicyPort;
import SA.irms.common.notification.NotificationCommandPublisher;
import SA.irms.common.context.RequestMetadata;
import SA.irms.common.notification.NotificationCommand;

@Service
public class BillingRefundReviewService {
    private final RefundReviewRepository repository;
    private final SharedIdentityPolicyPort identityPolicyPort;
    private final AuditRecorder auditService;
    private final NotificationCommandPublisher notificationOutboxPublisher;
    private final BillingReadService billingReadService;
    private final BillingRefundPolicy refundPolicy;
    private final BillingRefundExecutionService executionService;

    public BillingRefundReviewService(RefundReviewRepository repository,
                                      SharedIdentityPolicyPort identityPolicyPort,
                                      AuditRecorder auditService,
                                      NotificationCommandPublisher notificationOutboxPublisher,
                                      BillingReadService billingReadService,
                                      BillingRefundPolicy refundPolicy,
                                      BillingRefundExecutionService executionService) {
        this.repository = repository;
        this.identityPolicyPort = identityPolicyPort;
        this.auditService = auditService;
        this.notificationOutboxPublisher = notificationOutboxPublisher;
        this.billingReadService = billingReadService;
        this.refundPolicy = refundPolicy;
        this.executionService = executionService;
    }

    @Transactional
    public SA.irms.billing.application.view.BillingViews.RefundView reviewRefund(UUID refundId, SA.irms.billing.application.command.BillingCommands.RefundApprovalRequest request, AuthenticatedUser actor, String correlationId, RequestMetadata httpServletRequest) {
        if (!refundPolicy.canApproveRefund(actor)) {
            throw new ConflictException("Manager approval is required to review queued refunds.");
        }
        SA.irms.billing.application.view.BillingReadModels.BillingRefundRecord refund = billingReadService.loadRefundRecord(refundId);
        if (!List.of("requested", "pending_review").contains(refund.status())) {
            throw new ConflictException("Only queued refunds can be reviewed.");
        }
        String action = request.action() == null ? "" : request.action().trim().toLowerCase();
        if ("reject".equals(action) || "rejected".equals(action)) {
            return rejectRefund(refundId, refund, request, actor, correlationId, httpServletRequest.remoteIp());
        }
        if (!"approve".equals(action) && !"approved".equals(action)) {
            throw new ConflictException("Refund review action must be approve or reject.");
        }
        PaymentRecord payment = billingReadService.loadPaymentRecord(refund.paymentId());
        BigDecimal refundable = executionService.calculateRefundableBalance(payment.paymentId(), payment.amount(), refundId);
        if (refund.amount().compareTo(refundable) > 0) {
            throw new ConflictException("Refund amount exceeds the current refundable balance.");
        }
        refundPolicy.ensureRefundWindowOpen(payment, identityPolicyPort.getPolicySnapshot().authorizationPolicy());
        return executionService.executeRefund(payment, refund.amount(), refund.reason(), actor, correlationId, httpServletRequest, refundId);
    }

    private SA.irms.billing.application.view.BillingViews.RefundView rejectRefund(UUID refundId, SA.irms.billing.application.view.BillingReadModels.BillingRefundRecord refund,
            SA.irms.billing.application.command.BillingCommands.RefundApprovalRequest request, AuthenticatedUser actor, String correlationId, String remoteAddress) {
        repository.markRejected(refundId, actor.userId());
        auditService.record(actor.userId(), "billing.refund.rejected", "Refund", refundId.toString(), correlationId,
                request.reason() == null || request.reason().isBlank() ? "Refund rejected." : request.reason(), true,
                remoteAddress, Map.of("status", refund.status(), "amount", refund.amount()),
                Map.of("status", "rejected", "amount", refund.amount()));
        notificationOutboxPublisher.enqueue(new NotificationCommand(null, null, null, refund.paymentId(), "in_app", "refund",
                "REFUND_REJECTED", Map.of("refundId", refundId.toString(), "billId", refund.billId().toString()),
                "Refund request rejected", "Your refund request for bill " + refund.billId() + " was rejected.",
                null, refund.requestedBy(), null, "high"), "Refund", refundId.toString(), "billing-refund-" + refundId);
        return billingReadService.findRefund(refundId);
    }
}
