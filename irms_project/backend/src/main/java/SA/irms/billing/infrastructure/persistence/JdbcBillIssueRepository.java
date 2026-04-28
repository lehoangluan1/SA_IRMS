package SA.irms.billing.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.billing.application.port.out.BillIssueRepository;

@Repository
public class JdbcBillIssueRepository implements BillIssueRepository {
    private final JdbcClient jdbcClient;

    public JdbcBillIssueRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<UUID> findExistingBillId(UUID tableSessionId) {
        return jdbcClient.sql("select bill_id from bills where table_session_id = :tableSessionId")
                .param("tableSessionId", tableSessionId)
                .query(UUID.class)
                .optional();
    }

    @Override
    public long countUnreadyItems(UUID tableSessionId) {
        return jdbcClient.sql("""
                        select count(*)
                        from orders o
                        join order_items oi on oi.order_id = o.order_id
                        where o.table_session_id = :tableSessionId
                          and oi.line_status not in ('ready', 'served', 'cancelled')
                        """)
                .param("tableSessionId", tableSessionId)
                .query(Long.class)
                .single();
    }

    @Override
    public List<BillLineSource> loadBillLineSources(UUID tableSessionId) {
        return jdbcClient.sql("""
                        select oi.order_item_id,
                               oi.snapshot_name,
                               oi.quantity,
                               oi.unit_price,
                               coalesce((
                                   select sum(oim.extra_price * oim.qty_multiplier)
                                   from order_item_modifiers oim
                                   where oim.order_item_id = oi.order_item_id
                               ), 0) as modifier_total,
                               (
                                   select string_agg(oim.name_snapshot, ', ' order by oim.created_at)
                                   from order_item_modifiers oim
                                   where oim.order_item_id = oi.order_item_id
                               ) as modifiers
                        from orders o
                        join order_items oi on oi.order_id = o.order_id
                        where o.table_session_id = :tableSessionId
                          and oi.line_status <> 'cancelled'
                        order by oi.created_at
                        """)
                .param("tableSessionId", tableSessionId)
                .query((rs, rowNum) -> new BillLineSource(
                        rs.getObject("order_item_id", UUID.class),
                        rs.getString("snapshot_name"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("unit_price"),
                        rs.getBigDecimal("modifier_total"),
                        rs.getString("modifiers")
                ))
                .list();
    }

    @Override
    public void insertBill(UUID billId, UUID tableSessionId, BigDecimal subTotal, BigDecimal taxAmount,
                           BigDecimal serviceFee, BigDecimal grandTotal, String correlationId) {
        jdbcClient.sql("""
                        insert into bills (
                            bill_id,
                            table_session_id,
                            status,
                            sub_total,
                            tax_amount,
                            service_fee,
                            tip_amount,
                            grand_total,
                            discount_total,
                            correlation_id,
                            created_at,
                            finalized_at
                        ) values (
                            :billId,
                            :tableSessionId,
                            'finalized',
                            :subTotal,
                            :taxAmount,
                            :serviceFee,
                            0,
                            :grandTotal,
                            0,
                            :correlationId,
                            now(),
                            now()
                        )
                        """)
                .param("billId", billId)
                .param("tableSessionId", tableSessionId)
                .param("subTotal", subTotal)
                .param("taxAmount", taxAmount)
                .param("serviceFee", serviceFee)
                .param("grandTotal", grandTotal)
                .param("correlationId", correlationId)
                .update();
    }

    @Override
    public void insertBillLine(UUID billId, UUID sourceOrderItemId, String label, int quantity, BigDecimal lineTotal) {
        jdbcClient.sql("""
                        insert into bill_lines (
                            bill_line_id,
                            bill_id,
                            source_order_item_id,
                            label,
                            quantity,
                            line_total
                        ) values (
                            :billLineId,
                            :billId,
                            :sourceOrderItemId,
                            :label,
                            :quantity,
                            :lineTotal
                        )
                        """)
                .param("billLineId", UUID.randomUUID())
                .param("billId", billId)
                .param("sourceOrderItemId", sourceOrderItemId)
                .param("label", label)
                .param("quantity", quantity)
                .param("lineTotal", lineTotal)
                .update();
    }

    @Override
    public void insertFullBillSplit(UUID billId, BigDecimal allocatedTotal) {
        jdbcClient.sql("""
                        insert into bill_splits (split_id, bill_id, label, status, allocated_total, tip_amount)
                        values (:splitId, :billId, 'Full Bill', 'pending', :allocatedTotal, 0)
                        """)
                .param("splitId", UUID.randomUUID())
                .param("billId", billId)
                .param("allocatedTotal", allocatedTotal)
                .update();
    }

    @Override
    public void updateTableSessionToBilling(UUID tableSessionId) {
        jdbcClient.sql("""
                        update table_sessions
                        set status = 'billing',
                            updated_at = now()
                        where session_id = :tableSessionId
                        """)
                .param("tableSessionId", tableSessionId)
                .update();
    }
}
