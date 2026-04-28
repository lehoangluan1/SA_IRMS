package SA.irms.billing.infrastructure.persistence;

import SA.irms.billing.application.port.out.BillingReceiptRepository;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBillingReceiptRepository implements BillingReceiptRepository {
    private final JdbcClient jdbcClient;

    JdbcBillingReceiptRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean receiptExists(UUID paymentId) {
        return jdbcClient.sql("select count(*) from receipts where payment_id = :paymentId")
                .param("paymentId", paymentId)
                .query(Long.class)
                .single() > 0;
    }

    @Override
    public UUID findBillIdByPayment(UUID paymentId) {
        return jdbcClient.sql("select bill_id from payments where payment_id = :paymentId")
                .param("paymentId", paymentId)
                .query(UUID.class)
                .single();
    }

    @Override
    public void insertReceipt(UUID receiptId, UUID paymentId, UUID billId, String deliveryChannel, String recipientAddress) {
        jdbcClient.sql("""
                        insert into receipts (
                            receipt_id,
                            payment_id,
                            bill_id,
                            issued_at,
                            delivery_channel,
                            document_no,
                            recipient_address
                        ) values (
                            :receiptId,
                            :paymentId,
                            :billId,
                            now(),
                            :deliveryChannel,
                            :documentNo,
                            :recipientAddress
                        )
                        """)
                .param("receiptId", receiptId)
                .param("paymentId", paymentId)
                .param("billId", billId)
                .param("deliveryChannel", deliveryChannel)
                .param("documentNo", "RCP-" + paymentId.toString().substring(0, 8))
                .param("recipientAddress", recipientAddress)
                .update();
    }

    @Override
    public void updateReceiptDelivery(UUID paymentId, String deliveryChannel, String recipientAddress) {
        jdbcClient.sql("""
                        update receipts
                        set delivery_channel = :deliveryChannel,
                            recipient_address = :recipientAddress
                        where payment_id = :paymentId
                        """)
                .param("deliveryChannel", deliveryChannel)
                .param("recipientAddress", recipientAddress)
                .param("paymentId", paymentId)
                .update();
    }
}
