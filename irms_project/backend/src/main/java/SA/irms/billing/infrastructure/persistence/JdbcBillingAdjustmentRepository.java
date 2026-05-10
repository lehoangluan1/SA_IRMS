package SA.irms.billing.infrastructure.persistence;

import SA.irms.billing.application.port.out.BillAdjustmentRepository;
import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBillingAdjustmentRepository implements BillAdjustmentRepository {
    private final JdbcClient jdbcClient;

    JdbcBillingAdjustmentRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void updateBillTotals(UUID billId, BigDecimal tipAmount, BigDecimal discountAmount) {
        jdbcClient.sql("""
                        update bills
                        set tip_amount = :tipAmount,
                            discount_total = :discountAmount,
                            grand_total = sub_total + tax_amount + service_fee + :tipAmount - :discountAmount,
                            updated_at = now()
                        where bill_id = :billId
                        """)
                .param("tipAmount", tipAmount)
                .param("discountAmount", discountAmount)
                .param("billId", billId)
                .update();
    }

    @Override
    public void syncSingleSplit(UUID billId, BigDecimal tipAmount) {
        jdbcClient.sql("""
                        update bill_splits
                        set allocated_total = (select grand_total from bills where bill_id = :billId),
                            tip_amount = :tipAmount,
                            updated_at = now()
                        where bill_id = :billId
                          and label = 'Full Bill'
                          and not exists (
                              select 1
                              from bill_splits other
                              where other.bill_id = :billId
                                and other.label <> 'Full Bill'
                          )
                        """)
                .param("tipAmount", tipAmount)
                .param("billId", billId)
                .update();
    }
}
