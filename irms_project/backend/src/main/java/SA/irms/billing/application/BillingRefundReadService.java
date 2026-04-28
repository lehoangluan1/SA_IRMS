package SA.irms.billing.application;

import SA.irms.billing.application.support.BillingRelativeTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import SA.irms.billing.application.view.BillingReadModels;
import SA.irms.billing.application.view.BillingViews;
import SA.irms.common.error.NotFoundException;

@Service
class BillingRefundReadService {
    private final JdbcClient jdbcClient;
    private final BillingRelativeTime relativeTime;

    BillingRefundReadService(JdbcClient jdbcClient, BillingRelativeTime relativeTime) {
        this.jdbcClient = jdbcClient;
        this.relativeTime = relativeTime;
    }

    BillingReadModels.BillingRefundRecord loadRefundRecord(UUID refundId) {
        return jdbcClient.sql("""
                        select refund_id, payment_id, bill_id, amount, reason, status, requested_by
                        from refunds
                        where refund_id = :refundId
                        """)
                .param("refundId", refundId)
                .query((rs, rowNum) -> new BillingReadModels.BillingRefundRecord(
                        rs.getObject("refund_id", UUID.class), rs.getObject("payment_id", UUID.class), rs.getObject("bill_id", UUID.class),
                        rs.getBigDecimal("amount"), rs.getString("reason"), rs.getString("status"), rs.getObject("requested_by", UUID.class)
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Refund was not found."));
    }

    BillingViews.RefundView findRefund(UUID refundId) {
        return jdbcClient.sql(refundSelectSql() + " where r.refund_id = :refundId")
                .param("refundId", refundId)
                .query((rs, rowNum) -> mapRefundView(rs))
                .optional()
                .orElseThrow(() -> new NotFoundException("Refund was not found."));
    }

    List<BillingViews.RefundView> pendingRefunds() {
        return jdbcClient.sql(refundSelectSql() + " where r.status in ('requested', 'pending_review') order by r.created_at desc")
                .query((rs, rowNum) -> mapRefundView(rs))
                .list();
    }

    private BillingViews.RefundView mapRefundView(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new BillingViews.RefundView(
                rs.getObject("refund_id", UUID.class), rs.getObject("payment_id", UUID.class), rs.getObject("bill_id", UUID.class),
                rs.getBigDecimal("amount"), rs.getString("reason"), rs.getString("status"), rs.getString("requested_by"),
                rs.getString("approved_by"), rs.getString("payment_method"), SA.irms.common.support.TableCodeParser.parseTableNumber(rs.getString("table_code")),
                relativeTime.format(rs.getTimestamp("created_at").toInstant())
        );
    }

    private String refundSelectSql() {
        return """
                        select r.refund_id, r.payment_id, r.bill_id, r.amount, r.reason, r.status,
                               requester.display_name as requested_by, approver.display_name as approved_by,
                               p.method as payment_method, dt.code as table_code, r.created_at
                        from refunds r
                        join payments p on p.payment_id = r.payment_id
                        join bills b on b.bill_id = r.bill_id
                        join table_sessions ts on ts.session_id = b.table_session_id
                        join dining_tables dt on dt.table_id = ts.table_id
                        join users requester on requester.user_id = r.requested_by
                        left join users approver on approver.user_id = r.approved_by
                       """;
    }
}
