package SA.irms.billing.infrastructure.persistence;

import SA.irms.billing.application.port.out.BillSplitRepository;
import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBillingSplitRepository implements BillSplitRepository {
    private final JdbcClient jdbcClient;

    JdbcBillingSplitRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public long countPaidSplits(UUID billId) {
        return jdbcClient.sql("""
                        select count(*)
                        from bill_splits
                        where bill_id = :billId
                          and status = 'paid'
                        """)
                .param("billId", billId)
                .query(Long.class)
                .single();
    }

    @Override
    public void clearSplits(UUID billId) {
        jdbcClient.sql("delete from split_allocations where split_id in (select split_id from bill_splits where bill_id = :billId)")
                .param("billId", billId)
                .update();
        jdbcClient.sql("delete from bill_splits where bill_id = :billId")
                .param("billId", billId)
                .update();
    }

    @Override
    public int loadGuestCount(UUID billId) {
        return jdbcClient.sql("""
                        select guest_count
                        from table_sessions
                        where session_id = (select table_session_id from bills where bill_id = :billId)
                        """)
                .param("billId", billId)
                .query(Integer.class)
                .single();
    }

    @Override
    public void insertSplit(UUID splitId, UUID billId, String label, BigDecimal allocatedTotal, BigDecimal tipAmount) {
        jdbcClient.sql("""
                        insert into bill_splits (split_id, bill_id, label, status, allocated_total, tip_amount)
                        values (:splitId, :billId, :label, 'pending', :allocatedTotal, :tipAmount)
                        """)
                .param("splitId", splitId)
                .param("billId", billId)
                .param("label", label)
                .param("allocatedTotal", allocatedTotal)
                .param("tipAmount", tipAmount)
                .update();
    }

    @Override
    public void insertSplitAllocation(UUID splitId, UUID billLineId, BigDecimal amount, BigDecimal ratio) {
        jdbcClient.sql("""
                        insert into split_allocations (allocation_id, split_id, bill_line_id, amount, ratio)
                        values (:allocationId, :splitId, :billLineId, :amount, :ratio)
                        """)
                .param("allocationId", UUID.randomUUID())
                .param("splitId", splitId)
                .param("billLineId", billLineId)
                .param("amount", amount)
                .param("ratio", ratio)
                .update();
    }
}
