package SA.irms.billing.infrastructure.persistence;

import SA.irms.billing.application.port.out.PaymentQueryRepository;
import SA.irms.billing.application.support.BillingRelativeTime;
import SA.irms.billing.application.view.PaymentRecord;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.common.error.NotFoundException;

@Repository
public class JdbcBillingPaymentQueryRepository implements PaymentQueryRepository {
    private final JdbcClient jdbcClient;
    private final BillingRelativeTime relativeTime;

    JdbcBillingPaymentQueryRepository(JdbcClient jdbcClient, BillingRelativeTime relativeTime) {
        this.jdbcClient = jdbcClient;
        this.relativeTime = relativeTime;
    }

    public SA.irms.billing.application.view.BillingViews.PaymentView findPayment(UUID paymentId) {
        PaymentRecord paymentRecord = loadPaymentRecord(paymentId);
        return new SA.irms.billing.application.view.BillingViews.PaymentView(paymentRecord.paymentId(), paymentRecord.billId(), paymentRecord.splitId(),
                paymentRecord.splitLabel(), paymentRecord.method(), paymentRecord.amount(), paymentRecord.status(), relativeTime.format(paymentRecord.paidAt()));
    }

    public SA.irms.billing.application.view.BillingViews.ReceiptView findReceipt(UUID paymentId) {
        return jdbcClient.sql("""
                        select receipt_id, bill_id, delivery_channel, issued_at, recipient_address
                        from receipts
                        where payment_id = :paymentId
                        """)
                .param("paymentId", paymentId)
                .query((rs, rowNum) -> new SA.irms.billing.application.view.BillingViews.ReceiptView(
                        rs.getObject("receipt_id", UUID.class),
                        rs.getObject("bill_id", UUID.class),
                        rs.getString("delivery_channel"),
                        rs.getTimestamp("issued_at").toInstant(),
                        rs.getString("recipient_address")
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Receipt was not found."));
    }

    public List<SA.irms.billing.application.view.BillingViews.PaymentView> loadPayments(UUID billId) {
        return jdbcClient.sql("""
                        select p.payment_id, p.bill_id, p.split_id, bs.label as split_label, p.method, p.amount, p.status, p.paid_at
                        from payments p
                        left join bill_splits bs on bs.split_id = p.split_id
                        where p.bill_id = :billId
                        order by p.paid_at desc
                        """)
                .param("billId", billId)
                .query((rs, rowNum) -> new SA.irms.billing.application.view.BillingViews.PaymentView(
                        rs.getObject("payment_id", UUID.class), rs.getObject("bill_id", UUID.class), rs.getObject("split_id", UUID.class),
                        rs.getString("split_label"), rs.getString("method"), rs.getBigDecimal("amount"), rs.getString("status"),
                        relativeTime.format(rs.getTimestamp("paid_at").toInstant())
                ))
                .list();
    }

    public PaymentRecord loadPaymentRecord(UUID paymentId) {
        return jdbcClient.sql("""
                        select p.payment_id, p.bill_id, p.split_id, bs.label as split_label, p.method, p.amount,
                               p.status, p.gateway_ref, p.paid_at
                        from payments p
                        left join bill_splits bs on bs.split_id = p.split_id
                        where p.payment_id = :paymentId
                        """)
                .param("paymentId", paymentId)
                .query((rs, rowNum) -> new PaymentRecord(
                        rs.getObject("payment_id", UUID.class), rs.getObject("bill_id", UUID.class), rs.getObject("split_id", UUID.class),
                        rs.getString("split_label"), rs.getString("method"), rs.getBigDecimal("amount"), rs.getString("status"),
                        rs.getString("gateway_ref"), rs.getTimestamp("paid_at").toInstant()
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Payment was not found."));
    }
}