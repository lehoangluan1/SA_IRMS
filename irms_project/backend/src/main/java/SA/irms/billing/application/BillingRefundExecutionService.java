package SA.irms.billing.application;

import SA.irms.billing.application.view.PaymentRecord;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import SA.irms.billing.application.port.out.RefundExecutionRepository;
import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.error.ConflictException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.billing.application.events.RefundIssuedEvent;
import SA.irms.common.notification.NotificationCommand;
import SA.irms.common.notification.NotificationCommandPublisher;
import SA.irms.common.outbox.DomainEventPublisher;
import SA.irms.common.context.RequestMetadata;

@Service
public class BillingRefundExecutionService {
    private final RefundExecutionRepository repository;
    private final AuditRecorder auditService;
    private final NotificationCommandPublisher notificationOutboxPublisher;
    private final BillingReadService billingReadService;
    private final List<PaymentGateway> paymentGateways;
    private final DomainEventPublisher outboxPublisher;

    public BillingRefundExecutionService(
            RefundExecutionRepository repository,
            AuditRecorder auditService,
            NotificationCommandPublisher notificationOutboxPublisher,
            BillingReadService billingReadService,
            List<PaymentGateway> paymentGateways,
            DomainEventPublisher outboxPublisher
    ) {
        this.repository = repository;
        this.auditService = auditService;
        this.notificationOutboxPublisher = notificationOutboxPublisher;
        this.billingReadService = billingReadService;
        this.paymentGateways = paymentGateways;
        this.outboxPublisher = outboxPublisher;
    }

    public SA.irms.billing.application.view.BillingViews.RefundView executeRefund(PaymentRecord payment, BigDecimal amount, String reason,
            AuthenticatedUser actor, String correlationId, RequestMetadata request, UUID existingRefundId) {
        UUID refundId = existingRefundId == null ? UUID.randomUUID() : existingRefundId;
        PaymentGateway.RefundResult refundResult = resolvePaymentGateway(payment.method()).refund(payment, amount, refundId);
        if (existingRefundId == null) {
            repository.insertRefund(refundId, payment.paymentId(), payment.billId(), amount, reason, refundResult.status(), actor.userId(),
                    "completed".equals(refundResult.status()) ? actor.userId() : null, refundResult.externalReference());
        } else {
            repository.updateQueuedRefund(refundId, refundResult.status(), actor.userId(), refundResult.externalReference());
        }
        if ("completed".equals(refundResult.status())) {
            repository.markPaymentRefundedIfFullyRefunded(payment.paymentId(), amount);
            BigDecimal remaining = payment.amount().subtract(repository.calculateRefundableBalance(payment.paymentId(), payment.amount(), null));
            repository.updateBillSettlementStatus(payment.billId(), remaining.compareTo(BigDecimal.ZERO) <= 0 ? "paid" : "partially_paid",
                    remaining.compareTo(BigDecimal.ZERO) <= 0);
            outboxPublisher.publish(new RefundIssuedEvent(refundId.toString(), Map.of(
                    "refundId", refundId.toString(),
                    "paymentId", payment.paymentId().toString(),
                    "billId", payment.billId().toString(),
                    "amount", amount,
                    "reason", reason == null ? "" : reason,
                    "externalReference", refundResult.externalReference() == null ? "" : refundResult.externalReference()
            )), correlationId, null);
        }
        auditRefund(payment, amount, reason, actor, correlationId, request, existingRefundId, refundId, refundResult);
        queueCompletionNotification(payment, refundId, existingRefundId, refundResult);
        return billingReadService.findRefund(refundId);
    }

    public BigDecimal calculateRefundableBalance(UUID paymentId, BigDecimal paymentAmount, UUID excludedRefundId) {
        return repository.calculateRefundableBalance(paymentId, paymentAmount, excludedRefundId);
    }

    private void auditRefund(PaymentRecord payment, BigDecimal amount, String reason, AuthenticatedUser actor,
            String correlationId, RequestMetadata request, UUID existingRefundId, UUID refundId, PaymentGateway.RefundResult result) {
        Map<String, Object> afterPayload = new java.util.LinkedHashMap<>();
        afterPayload.put("status", result.status());
        afterPayload.put("refundAmount", amount);
        if (result.failureReason() != null) {
            afterPayload.put("failureReason", result.failureReason());
        }
        auditService.record(actor.userId(), existingRefundId == null ? "billing.refund.issued" : "billing.refund.approved",
                existingRefundId == null ? "Payment" : "Refund", existingRefundId == null ? payment.paymentId().toString() : refundId.toString(),
                correlationId, reason, true, request.remoteIp(), Map.of("status", payment.status(), "amount", payment.amount()), afterPayload);
    }

    private void queueCompletionNotification(PaymentRecord payment, UUID refundId, UUID existingRefundId, PaymentGateway.RefundResult refundResult) {
        UUID recipient = existingRefundId == null ? null : billingReadService.loadRefundRecord(refundId).requestedBy();
        if (existingRefundId == null) {
            recipient = billingReadService.loadRefundRecord(refundId).requestedBy();
        }
        notificationOutboxPublisher.enqueue(new NotificationCommand(null, null, null, payment.paymentId(), "in_app", "refund",
                existingRefundId == null ? "REFUND_COMPLETED" : "REFUND_APPROVED",
                Map.of("refundId", refundId.toString(), "billId", payment.billId().toString()), "Refund updated",
                "Refund processing for bill " + payment.billId() + " is now " + refundResult.status() + ".",
                null, recipient, null, "high"), "Payment", payment.paymentId().toString(), "billing-refund-" + refundId);
    }

    private PaymentGateway resolvePaymentGateway(String method) {
        return paymentGateways.stream()
                .filter(gateway -> gateway.supports(method))
                .findFirst()
                .orElseThrow(() -> new ConflictException("Unsupported payment method."));
    }
}
