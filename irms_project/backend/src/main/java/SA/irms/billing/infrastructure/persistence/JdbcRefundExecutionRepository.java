package SA.irms.billing.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.billing.application.port.out.RefundExecutionRepository;

@Repository
public class JdbcRefundExecutionRepository implements RefundExecutionRepository {
    private final JdbcClient jdbcClient;

    public JdbcRefundExecutionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public BigDecimal calculateRefundableBalance(UUID paymentId, BigDecimal paymentAmount, UUID excludedRefundId) {
        String excludedPredicate = excludedRefundId == null ? "" : "and refund_id <> :excludedRefundId";
        JdbcClient.StatementSpec statement = jdbcClient.sql("""
                        select coalesce(sum(amount), 0)
                        from refunds
                        where payment_id = :paymentId
                          and status in ('requested', 'pending_review', 'approved', 'completed')
                          %s
                        """.formatted(excludedPredicate))
                .param("paymentId", paymentId);
        if (excludedRefundId != null) {
            statement = statement.param("excludedRefundId", excludedRefundId);
        }
        BigDecimal alreadyRefundedOrQueued = statement.query(BigDecimal.class).single();
        return paymentAmount.subtract(alreadyRefundedOrQueued);
    }

    @Override
    public void insertRefund(UUID refundId, UUID paymentId, UUID billId, BigDecimal amount, String reason,
                             String status, UUID requestedBy, UUID approvedBy, String externalTransactionRef) {
        jdbcClient.sql("""
                        insert into refunds (refund_id, payment_id, bill_id, amount, reason, status, requested_by,
                                             approved_by, processed_at, external_transaction_ref)
                        values (:refundId, :paymentId, :billId, :amount, :reason, :status, :requestedBy,
                                :approvedBy, now(), :externalTransactionRef)
                        """)
                .param("refundId", refundId)
                .param("paymentId", paymentId)
                .param("billId", billId)
                .param("amount", amount)
                .param("reason", reason)
                .param("status", status)
                .param("requestedBy", requestedBy)
                .param("approvedBy", approvedBy)
                .param("externalTransactionRef", externalTransactionRef)
                .update();
    }

    @Override
    public void updateQueuedRefund(UUID refundId, String status, UUID approvedBy, String externalTransactionRef) {
        jdbcClient.sql("""
                        update refunds
                        set status = :status, approved_by = :approvedBy, processed_at = now(),
                            external_transaction_ref = :externalTransactionRef, updated_at = now()
                        where refund_id = :refundId
                        """)
                .param("status", status)
                .param("approvedBy", approvedBy)
                .param("externalTransactionRef", externalTransactionRef)
                .param("refundId", refundId)
                .update();
    }

    @Override
    public void markPaymentRefundedIfFullyRefunded(UUID paymentId, BigDecimal amount) {
        jdbcClient.sql("""
                        update payments
                        set status = case when amount <= :amount then 'refunded' else status end,
                            updated_at = now()
                        where payment_id = :paymentId
                        """)
                .param("amount", amount)
                .param("paymentId", paymentId)
                .update();
    }

    @Override
    public void updateBillSettlementStatus(UUID billId, String status, boolean closeBill) {
        jdbcClient.sql("""
                        update bills
                        set status = :status,
                            closed_at = case when :closeBill then now() else closed_at end,
                            updated_at = now()
                        where bill_id = :billId
                        """)
                .param("status", status)
                .param("closeBill", closeBill)
                .param("billId", billId)
                .update();
    }
}
