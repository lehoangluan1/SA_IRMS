package SA.irms.billing.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.billing.application.port.out.BillPaymentRepository;

@Repository
public class JdbcBillPaymentRepository implements BillPaymentRepository {
    private final JdbcClient jdbcClient;

    public JdbcBillPaymentRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public BigDecimal calculateOutstanding(UUID billId, UUID splitId) {
        if (splitId != null) {
            BigDecimal allocated = jdbcClient.sql("select allocated_total from bill_splits where split_id = :splitId")
                    .param("splitId", splitId)
                    .query(BigDecimal.class)
                    .single();
            BigDecimal paid = jdbcClient.sql("""
                            select coalesce(sum(amount), 0)
                            from payments
                            where split_id = :splitId
                              and status = 'completed'
                            """)
                    .param("splitId", splitId)
                    .query(BigDecimal.class)
                    .single();
            return allocated.subtract(paid);
        }
        BigDecimal grandTotal = jdbcClient.sql("select grand_total from bills where bill_id = :billId")
                .param("billId", billId)
                .query(BigDecimal.class)
                .single();
        BigDecimal paid = jdbcClient.sql("""
                        select coalesce(sum(amount), 0)
                        from payments
                        where bill_id = :billId
                          and status = 'completed'
                        """)
                .param("billId", billId)
                .query(BigDecimal.class)
                .single();
        BigDecimal refunded = jdbcClient.sql("""
                        select coalesce(sum(amount), 0)
                        from refunds
                        where bill_id = :billId
                          and status = 'completed'
                        """)
                .param("billId", billId)
                .query(BigDecimal.class)
                .single();
        return grandTotal.subtract(paid).add(refunded);
    }

    @Override
    public void recordPayment(UUID paymentId, UUID billId, UUID splitId, String method, BigDecimal amount,
                              String status, String gatewayRef, BigDecimal cashReceived, BigDecimal changeDue,
                              UUID processedByUserId) {
        jdbcClient.sql("""
                        insert into payments (
                            payment_id,
                            bill_id,
                            split_id,
                            method,
                            amount,
                            status,
                            gateway_ref,
                            cash_received,
                            change_due,
                            processed_by_user_id,
                            paid_at
                        ) values (
                            :paymentId,
                            :billId,
                            :splitId,
                            :method,
                            :amount,
                            :status,
                            :gatewayRef,
                            :cashReceived,
                            :changeDue,
                            :processedByUserId,
                            now()
                        )
                        """)
                .param("paymentId", paymentId)
                .param("billId", billId)
                .param("splitId", splitId)
                .param("method", method)
                .param("amount", amount)
                .param("status", status)
                .param("gatewayRef", gatewayRef)
                .param("cashReceived", cashReceived)
                .param("changeDue", changeDue)
                .param("processedByUserId", processedByUserId)
                .update();
    }

    @Override
    public void markSplitPaid(UUID splitId) {
        jdbcClient.sql("""
                        update bill_splits
                        set status = 'paid',
                            updated_at = now()
                        where split_id = :splitId
                        """)
                .param("splitId", splitId)
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
