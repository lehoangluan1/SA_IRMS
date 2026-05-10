package SA.irms.billing.infrastructure.persistence;

import SA.irms.billing.application.port.out.RefundRequestRepository;
import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBillingRefundRequestRepository implements RefundRequestRepository {
    private final JdbcClient jdbcClient;

    JdbcBillingRefundRequestRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void createPendingRefund(UUID refundId, UUID paymentId, UUID billId, BigDecimal amount, String reason, UUID requestedBy) {
        jdbcClient.sql("""
                        insert into refunds (refund_id, payment_id, bill_id, amount, reason, status, requested_by)
                        values (:refundId, :paymentId, :billId, :amount, :reason, 'pending_review', :requestedBy)
                        """)
                .param("refundId", refundId)
                .param("paymentId", paymentId)
                .param("billId", billId)
                .param("amount", amount)
                .param("reason", reason)
                .param("requestedBy", requestedBy)
                .update();
    }
}
