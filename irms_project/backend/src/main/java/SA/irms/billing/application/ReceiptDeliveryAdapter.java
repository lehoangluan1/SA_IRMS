package SA.irms.billing.application;

import java.util.UUID;

public interface ReceiptDeliveryAdapter {
    boolean supports(String channel);

    void validate(String recipientAddress);

    void deliver(DeliveryRequest request);

    record DeliveryRequest(
            UUID paymentId,
            UUID billId,
            UUID receiptId,
            String channel,
            String recipientAddress
    ) {
    }
}
