package SA.irms.billing.application;

import org.springframework.stereotype.Component;

@Component
public class PrintReceiptDeliveryAdapter implements ReceiptDeliveryAdapter {
    @Override
    public boolean supports(String channel) {
        return "print".equalsIgnoreCase(channel);
    }

    @Override
    public void validate(String recipientAddress) {
        // Printing does not require a recipient address.
    }

    @Override
    public void deliver(DeliveryRequest request) {
        // Printing is synchronous at the counter; no queued notification is required.
    }
}
