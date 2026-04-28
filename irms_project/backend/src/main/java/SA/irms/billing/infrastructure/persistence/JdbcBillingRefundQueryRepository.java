package SA.irms.billing.infrastructure.persistence;

import SA.irms.billing.application.port.out.RefundQueryRepository;
import SA.irms.billing.application.support.BillingRelativeTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.common.error.NotFoundException;
import SA.irms.common.identity.SharedIdentityDirectoryPort;

@Repository
public class JdbcBillingRefundQueryRepository implements RefundQueryRepository {
    private final JdbcClient jdbcClient;
    private final BillingRelativeTime relativeTime;
    private final SharedIdentityDirectoryPort identityDirectoryPort;

    JdbcBillingRefundQueryRepository(
            JdbcClient jdbcClient,
            BillingRelativeTime relativeTime,
            SharedIdentityDirectoryPort identityDirectoryPort
    ) {
        this.jdbcClient = jdbcClient;
        this.relativeTime = relativeTime;
        this.identityDirectoryPort = identityDirectoryPort;
    }

    public SA.irms.billing.application.view.BillingReadModels.BillingRefundRecord loadRefundRecord(UUID refundId) {
        return jdbcClient.sql("""
                        select refund_id, payment_id, bill_id, amount, reason, status, requested_by
                        from refunds
                        where refund_id = :refundId
                        """)
                .param("refundId", refundId)
                .query((rs, rowNum) -> new SA.irms.billing.application.view.BillingReadModels.BillingRefundRecord(
                        rs.getObject("refund_id", UUID.class), rs.getObject("payment_id", UUID.class), rs.getObject("bill_id", UUID.class),
                        rs.getBigDecimal("amount"), rs.getString("reason"), rs.getString("status"), rs.getObject("requested_by", UUID.class)
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Refund was not found."));
    }

    public SA.irms.billing.application.view.BillingViews.RefundView findRefund(UUID refundId) {
        RefundRow row = jdbcClient.sql(refundSelectSql() + " where r.refund_id = :refundId")
                .param("refundId", refundId)
                .query((rs, rowNum) -> mapRefundRow(rs))
                .optional()
                .orElseThrow(() -> new NotFoundException("Refund was not found."));
        return toRefundView(row, loadDisplayNames(List.of(row)));
    }

    public List<SA.irms.billing.application.view.BillingViews.RefundView> pendingRefunds() {
        List<RefundRow> rows = jdbcClient.sql(refundSelectSql() + " where r.status in ('requested', 'pending_review') order by r.created_at desc")
                .query((rs, rowNum) -> mapRefundRow(rs))
                .list();
        Map<UUID, String> names = loadDisplayNames(rows);
        return rows.stream().map(row -> toRefundView(row, names)).toList();
    }

    private RefundRow mapRefundRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new RefundRow(
                rs.getObject("refund_id", UUID.class),
                rs.getObject("payment_id", UUID.class),
                rs.getObject("bill_id", UUID.class),
                rs.getBigDecimal("amount"),
                rs.getString("reason"),
                rs.getString("status"),
                rs.getObject("requested_by", UUID.class),
                rs.getObject("approved_by", UUID.class),
                rs.getString("payment_method"),
                SA.irms.common.support.TableCodeParser.parseTableNumber(rs.getString("table_code")),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private SA.irms.billing.application.view.BillingViews.RefundView toRefundView(RefundRow row, Map<UUID, String> names) {
        return new SA.irms.billing.application.view.BillingViews.RefundView(
                row.refundId(), row.paymentId(), row.billId(), row.amount(), row.reason(), row.status(),
                displayName(names, row.requestedBy()), displayName(names, row.approvedBy()), row.paymentMethod(), row.tableNumber(),
                relativeTime.format(row.createdAt())
        );
    }

    private Map<UUID, String> loadDisplayNames(List<RefundRow> rows) {
        Set<UUID> userIds = new LinkedHashSet<>();
        rows.forEach(row -> {
            if (row.requestedBy() != null) {
                userIds.add(row.requestedBy());
            }
            if (row.approvedBy() != null) {
                userIds.add(row.approvedBy());
            }
        });
        return identityDirectoryPort.findDisplayNames(userIds);
    }

    private String displayName(Map<UUID, String> names, UUID userId) {
        return userId == null ? null : names.getOrDefault(userId, userId.toString());
    }

    private String refundSelectSql() {
        return """
                        select r.refund_id, r.payment_id, r.bill_id, r.amount, r.reason, r.status,
                               r.requested_by, r.approved_by,
                               p.method as payment_method, dt.code as table_code, r.created_at
                        from refunds r
                        join payments p on p.payment_id = r.payment_id
                        join bills b on b.bill_id = r.bill_id
                        join table_sessions ts on ts.session_id = b.table_session_id
                        join dining_tables dt on dt.table_id = ts.table_id
                       """;
    }

    private record RefundRow(
            UUID refundId,
            UUID paymentId,
            UUID billId,
            java.math.BigDecimal amount,
            String reason,
            String status,
            UUID requestedBy,
            UUID approvedBy,
            String paymentMethod,
            int tableNumber,
            java.time.Instant createdAt
    ) {
    }
}