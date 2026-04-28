package SA.irms.billing.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.billing.application.port.out.BillPaymentRepository;
import SA.irms.common.error.ConflictException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.billing.application.events.PaymentCompletedEvent;
import SA.irms.common.outbox.DomainEventPublisher;

@Service
public class BillingPaymentService {
    private final BillPaymentRepository repository;
    private final BillingReadService billingReadService;
    private final List<PaymentGateway> paymentGateways;
    private final DomainEventPublisher outboxPublisher;

    public BillingPaymentService(
            BillPaymentRepository repository,
            BillingReadService billingReadService,
            List<PaymentGateway> paymentGateways,
            DomainEventPublisher outboxPublisher
    ) {
        this.repository = repository;
        this.billingReadService = billingReadService;
        this.paymentGateways = paymentGateways;
        this.outboxPublisher = outboxPublisher;
    }

    @Transactional
    public SA.irms.billing.application.view.BillingViews.PaymentView processPayment(UUID billId, SA.irms.billing.application.command.BillingCommands.PaymentRequest request, AuthenticatedUser actor) {
        String method = normalizePaymentMethod(request.method());
        BigDecimal outstanding = repository.calculateOutstanding(billId, request.splitId());
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException("The selected bill or split is already settled.");
        }
        BigDecimal paymentAmount = request.amount() == null ? outstanding : request.amount();
        if ("cash".equals(method)
                && request.amountReceived() != null
                && request.amountReceived().compareTo(paymentAmount) < 0) {
            throw new ConflictException("Cash received must cover the payment amount.");
        }
        UUID paymentId = UUID.randomUUID();
        PaymentGateway.PaymentAuthorizationResult authorizationResult = resolvePaymentGateway(method)
                .authorize(method, paymentAmount, request.amountReceived(), paymentId);
        repository.recordPayment(paymentId, billId, request.splitId(), method, paymentAmount, authorizationResult.status(),
                authorizationResult.gatewayReference(), authorizationResult.cashReceived(), authorizationResult.changeDue(), actor.userId());
        if (request.splitId() != null) {
            repository.markSplitPaid(request.splitId());
        }
        BigDecimal remaining = repository.calculateOutstanding(billId, null);
        repository.updateBillSettlementStatus(billId, remaining.compareTo(BigDecimal.ZERO) <= 0 ? "paid" : "partially_paid",
                remaining.compareTo(BigDecimal.ZERO) <= 0);
        if ("completed".equals(authorizationResult.status())) {
            outboxPublisher.publish(new PaymentCompletedEvent(paymentId.toString(), java.util.Map.of(
                    "paymentId", paymentId.toString(),
                    "billId", billId.toString(),
                    "splitId", request.splitId() == null ? "" : request.splitId().toString(),
                    "method", method,
                    "amount", paymentAmount,
                    "gatewayRef", authorizationResult.gatewayReference() == null ? "" : authorizationResult.gatewayReference()
            )), "payment-" + paymentId, null);
        }
        return billingReadService.findPayment(paymentId);
    }

    private String normalizePaymentMethod(String method) {
        if (method == null || method.isBlank()) {
            throw new ConflictException("A payment method is required.");
        }
        String normalized = method.trim().toLowerCase();
        if (!List.of("cash", "credit_card", "debit_card", "mobile_payment", "gift_card").contains(normalized)) {
            throw new ConflictException("Unsupported payment method.");
        }
        return normalized;
    }

    private PaymentGateway resolvePaymentGateway(String method) {
        return paymentGateways.stream()
                .filter(gateway -> gateway.supports(method))
                .findFirst()
                .orElseThrow(() -> new ConflictException("Unsupported payment method."));
    }
}
