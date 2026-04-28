package SA.irms.billing.application;

import SA.irms.billing.application.view.PaymentRecord;
import SA.irms.billing.application.port.out.RefundRequestRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.common.identity.PolicySnapshot;
import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.identity.SharedIdentityPolicyPort;
import SA.irms.common.notification.NotificationCommandPublisher;
import SA.irms.common.context.RequestMetadata;
import SA.irms.common.notification.NotificationCommand;

@Service
public class BillingRefundRequestService {
    private final RefundRequestRepository repository;
    private final SharedIdentityPolicyPort identityPolicyPort;
    private final AuditRecorder auditService;
    private final NotificationCommandPublisher notificationOutboxPublisher;
    private final BillingReadService billingReadService;
    private final BillingRefundPolicy refundPolicy;
    private final BillingRefundExecutionService executionService;

    public BillingRefundRequestService(RefundRequestRepository repository,
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
    public SA.irms.billing.application.view.BillingViews.RefundView refundPayment(UUID paymentId, SA.irms.billing.application.command.BillingCommands.RefundRequest request, AuthenticatedUser actor, String correlationId, RequestMetadata httpServletRequest) {
        PaymentRecord payment = billingReadService.loadPaymentRecord(paymentId);
        BigDecimal refundable = executionService.calculateRefundableBalance(paymentId, payment.amount(), null);
        BigDecimal amount = refundPolicy.normalizeRefundAmount(request.amount(), refundable);
        PolicySnapshot.AuthorizationPolicy authorizationPolicy = identityPolicyPort.getPolicySnapshot().authorizationPolicy();
        refundPolicy.ensureRefundWindowOpen(payment, authorizationPolicy);
        if (amount.compareTo(authorizationPolicy.maxRefundLimit()) > 0 && !refundPolicy.canApproveRefund(actor)) {
            return createPendingRefund(payment, amount, request.reason(), actor, correlationId, httpServletRequest.remoteIp());
        }
        return executionService.executeRefund(payment, amount, request.reason(), actor, correlationId, httpServletRequest, null);
    }

    private SA.irms.billing.application.view.BillingViews.RefundView createPendingRefund(PaymentRecord payment, BigDecimal amount, String reason,
            AuthenticatedUser actor, String correlationId, String remoteAddress) {
        UUID refundId = UUID.randomUUID();
        repository.createPendingRefund(refundId, payment.paymentId(), payment.billId(), amount, reason, actor.userId());
        auditService.record(actor.userId(), "billing.refund.requested", "Payment", payment.paymentId().toString(), correlationId,
                reason, true, remoteAddress, Map.of("status", payment.status(), "amount", payment.amount()),
                Map.of("status", "pending_review", "refundAmount", amount));
        for (String recipientRole : List.of("manager", "admin")) {
            notificationOutboxPublisher.enqueue(new NotificationCommand(null, null, null, payment.paymentId(), "in_app", "refund",
                    "REFUND_REVIEW_REQUIRED", Map.of("refundId", refundId.toString(), "billId", payment.billId().toString()),
                    "Refund review required", "A refund request is waiting for approval.", recipientRole, null, null, "high"),
                    "Refund", refundId.toString(), correlationId);
        }
        return billingReadService.findRefund(refundId);
    }
}
