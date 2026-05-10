package SA.irms.billing.infrastructure.persistence;

import SA.irms.billing.application.port.out.RefundReviewRepository;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBillingRefundReviewRepository implements RefundReviewRepository {
    private final JdbcClient jdbcClient;

    JdbcBillingRefundReviewRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void markRejected(UUID refundId, UUID approvedBy) {
        jdbcClient.sql("""
                        update refunds
                        set status = 'rejected', approved_by = :approvedBy, processed_at = now(), updated_at = now()
                        where refund_id = :refundId
                        """)
                .param("approvedBy", approvedBy)
                .param("refundId", refundId)
                .update();
    }
}
