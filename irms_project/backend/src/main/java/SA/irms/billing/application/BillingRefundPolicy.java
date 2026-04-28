package SA.irms.billing.application;

import SA.irms.billing.application.view.PaymentRecord;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import SA.irms.common.error.ConflictException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.common.identity.PolicySnapshot;

@Component
public class BillingRefundPolicy {
    private final Clock clock;

    BillingRefundPolicy(Clock clock) {
        this.clock = clock;
    }

    BigDecimal normalizeRefundAmount(BigDecimal requestedAmount, BigDecimal refundable) {
        BigDecimal amount = requestedAmount == null ? refundable : requestedAmount;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException("Refund amount must be greater than zero.");
        }
        if (amount.compareTo(refundable) > 0) {
            throw new ConflictException("Refund amount exceeds the refundable balance.");
        }
        return amount;
    }

    void ensureRefundWindowOpen(PaymentRecord payment, PolicySnapshot.AuthorizationPolicy authorizationPolicy) {
        if (Duration.between(payment.paidAt(), Instant.now(clock)).toHours() > authorizationPolicy.refundWindowHours()) {
            throw new ConflictException("The configured refund window has expired for this payment.");
        }
    }

    boolean canApproveRefund(AuthenticatedUser actor) {
        return actor.hasRole("manager") || actor.hasRole("admin") || actor.hasPermission("all");
    }
}
