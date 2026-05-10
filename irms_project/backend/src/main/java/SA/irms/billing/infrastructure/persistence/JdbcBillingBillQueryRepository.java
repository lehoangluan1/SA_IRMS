package SA.irms.billing.infrastructure.persistence;

import SA.irms.billing.application.port.out.BillQueryRepository;
import SA.irms.billing.application.port.out.PaymentQueryRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.common.error.NotFoundException;
import SA.irms.common.identity.SharedIdentityPolicyPort;

@Repository
public class JdbcBillingBillQueryRepository implements BillQueryRepository {
    private final JdbcClient jdbcClient;
    private final PaymentQueryRepository paymentReadService;
    private final SharedIdentityPolicyPort identityPolicyPort;

    JdbcBillingBillQueryRepository(
            JdbcClient jdbcClient,
            PaymentQueryRepository paymentReadService,
            SharedIdentityPolicyPort identityPolicyPort
    ) {
        this.jdbcClient = jdbcClient;
        this.paymentReadService = paymentReadService;
        this.identityPolicyPort = identityPolicyPort;
    }

    public SA.irms.billing.application.view.BillingViews.BillView findBill(UUID billId) {
        BillHeader header = jdbcClient.sql("""
                        select b.bill_id, b.table_session_id as table_session_id, dt.code as table_code, b.sub_total,
                               b.tax_amount, b.service_fee, b.discount_total, b.tip_amount, b.grand_total, b.status
                        from bills b
                        join table_sessions ts on ts.session_id = b.table_session_id
                        join dining_tables dt on dt.table_id = ts.table_id
                        where b.bill_id = :billId
                        """)
                .param("billId", billId)
                .query((rs, rowNum) -> new BillHeader(
                        rs.getObject("bill_id", UUID.class),
                        rs.getObject("table_session_id", UUID.class),
                        SA.irms.common.support.TableCodeParser.parseTableNumber(rs.getString("table_code")),
                        rs.getBigDecimal("sub_total"),
                        rs.getBigDecimal("tax_amount"),
                        rs.getBigDecimal("service_fee"),
                        rs.getBigDecimal("discount_total"),
                        rs.getBigDecimal("tip_amount"),
                        rs.getBigDecimal("grand_total"),
                        rs.getString("status")
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Bill was not found."));
        BigDecimal taxRate = identityPolicyPort.getPolicySnapshot().taxPolicy().taxRate();
        return new SA.irms.billing.application.view.BillingViews.BillView(header.billId(), header.tableSessionId(), header.tableNumber(), loadLines(billId),
                header.subTotal(), header.taxAmount(), taxRate, header.serviceFee(), header.discount(), header.tipAmount(),
                header.grandTotal(), header.status(), loadSplits(billId), paymentReadService.loadPayments(billId));
    }

    public UUID resolveCurrentBillId(UUID billId, UUID tableSessionId) {
        if (billId != null) {
            return billId;
        }
        if (tableSessionId != null) {
            return jdbcClient.sql("""
                            select bill_id from bills
                            where table_session_id = :tableSessionId
                            order by created_at desc
                            limit 1
                            """)
                    .param("tableSessionId", tableSessionId)
                    .query(UUID.class)
                    .optional()
                    .orElse(null);
        }
        return jdbcClient.sql("""
                        select bill_id from bills
                        where status in ('open', 'finalized', 'partially_paid')
                        order by created_at desc
                        limit 1
                        """)
                .query(UUID.class)
                .optional()
                .orElse(null);
    }

    private List<SA.irms.billing.application.view.BillingViews.LineView> loadLines(UUID billId) {
        return jdbcClient.sql("""
                        select bl.bill_line_id, bl.label, bl.quantity, bl.line_total,
                               (select string_agg(oim.name_snapshot, ', ' order by oim.created_at)
                                from order_item_modifiers oim where oim.order_item_id = bl.source_order_item_id) as modifiers
                        from bill_lines bl
                        where bl.bill_id = :billId
                        order by bl.created_at
                        """)
                .param("billId", billId)
                .query((rs, rowNum) -> new SA.irms.billing.application.view.BillingViews.LineView(
                        rs.getObject("bill_line_id", UUID.class), rs.getString("label"), rs.getInt("quantity"),
                        rs.getBigDecimal("line_total"), rs.getString("modifiers") == null ? "" : rs.getString("modifiers")
                ))
                .list();
    }

    private List<SA.irms.billing.application.view.BillingViews.SplitView> loadSplits(UUID billId) {
        return jdbcClient.sql("""
                        select split_id, label, allocated_total, tip_amount, status
                        from bill_splits
                        where bill_id = :billId
                        order by created_at
                        """)
                .param("billId", billId)
                .query((rs, rowNum) -> new SA.irms.billing.application.view.BillingViews.SplitView(
                        rs.getObject("split_id", UUID.class), rs.getString("label"), rs.getBigDecimal("allocated_total"),
                        rs.getBigDecimal("tip_amount"), rs.getString("status")
                ))
                .list();
    }

    private record BillHeader(UUID billId, UUID tableSessionId, int tableNumber, BigDecimal subTotal, BigDecimal taxAmount,
                              BigDecimal serviceFee, BigDecimal discount, BigDecimal tipAmount,
                              BigDecimal grandTotal, String status) {
    }
}