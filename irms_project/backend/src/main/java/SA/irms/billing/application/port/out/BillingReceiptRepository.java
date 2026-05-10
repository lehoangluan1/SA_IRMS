package SA.irms.billing.application.port.out;

import java.util.UUID;

public interface BillingReceiptRepository {
    boolean receiptExists(UUID paymentId);

    UUID findBillIdByPayment(UUID paymentId);

    void insertReceipt(UUID receiptId, UUID paymentId, UUID billId, String deliveryChannel, String recipientAddress);

    void updateReceiptDelivery(UUID paymentId, String deliveryChannel, String recipientAddress);
}
