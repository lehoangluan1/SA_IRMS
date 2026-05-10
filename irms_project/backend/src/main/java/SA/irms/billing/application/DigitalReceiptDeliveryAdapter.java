package SA.irms.billing.application;

import java.util.Map;

import org.springframework.stereotype.Component;

import SA.irms.common.error.ConflictException;
import SA.irms.common.notification.NotificationCommandPublisher;
import SA.irms.common.notification.NotificationCommand;

@Component
public class DigitalReceiptDeliveryAdapter implements ReceiptDeliveryAdapter {
    private final NotificationCommandPublisher notificationOutboxPublisher;

    public DigitalReceiptDeliveryAdapter(NotificationCommandPublisher notificationOutboxPublisher) {
        this.notificationOutboxPublisher = notificationOutboxPublisher;
    }

    @Override
    public boolean supports(String channel) {
        return "email".equalsIgnoreCase(channel) || "sms".equalsIgnoreCase(channel);
    }

    @Override
    public void validate(String recipientAddress) {
        if (recipientAddress == null || recipientAddress.isBlank()) {
            throw new ConflictException("A recipient address is required for digital receipt delivery.");
        }
    }

    @Override
    public void deliver(DeliveryRequest request) {
        notificationOutboxPublisher.enqueue(new NotificationCommand(
                null,
                null,
                null,
                request.paymentId(),
                request.channel().trim().toLowerCase(),
                "receipt_delivery",
                "RECEIPT_DELIVERY",
                Map.of(
                        "receiptId", request.receiptId().toString(),
                        "paymentId", request.paymentId().toString(),
                        "billId", request.billId().toString()
                ),
                "Receipt ready",
                "Receipt " + request.receiptId() + " has been prepared for delivery.",
                null,
                null,
                request.recipientAddress(),
                "medium"
        ), "Receipt", request.receiptId().toString(), "receipt-" + request.receiptId());
    }
}
