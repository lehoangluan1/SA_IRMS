package SA.irms.billing.application;

import SA.irms.billing.application.view.PaymentRecord;
import SA.irms.billing.application.port.out.BillingReceiptRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import SA.irms.adapters.pdf.ReceiptPdfRenderer;
import SA.irms.common.error.ConflictException;
import SA.irms.common.identity.SharedIdentityPolicyPort;
import SA.irms.common.identity.BranchView;
import SA.irms.billing.application.events.ReceiptGeneratedEvent;
import SA.irms.common.outbox.DomainEventPublisher;
import java.util.Map;

@Service
public class BillingReceiptService {
    private final BillingReceiptRepository repository;
    private final BillingReadService billingReadService;
    private final List<ReceiptDeliveryAdapter> receiptDeliveryAdapters;
    private final ReceiptPdfRenderer receiptPdfRenderer;
    private final SharedIdentityPolicyPort identityPolicyPort;
    private final DomainEventPublisher outboxPublisher;

    public BillingReceiptService(BillingReceiptRepository repository,
                                 BillingReadService billingReadService,
                                 List<ReceiptDeliveryAdapter> receiptDeliveryAdapters,
                                 ReceiptPdfRenderer receiptPdfRenderer,
                                 SharedIdentityPolicyPort identityPolicyPort,
                                 DomainEventPublisher outboxPublisher) {
        this.repository = repository;
        this.billingReadService = billingReadService;
        this.receiptDeliveryAdapters = receiptDeliveryAdapters;
        this.receiptPdfRenderer = receiptPdfRenderer;
        this.identityPolicyPort = identityPolicyPort;
        this.outboxPublisher = outboxPublisher;
    }

    @Transactional
    public SA.irms.billing.application.view.BillingViews.ReceiptView issueReceipt(UUID paymentId, SA.irms.billing.application.command.BillingCommands.ReceiptRequest request) {
        ReceiptDeliveryAdapter deliveryAdapter = resolveReceiptDeliveryAdapter(request.channel());
        deliveryAdapter.validate(request.recipientAddress());
        if (!repository.receiptExists(paymentId)) {
            UUID receiptId = UUID.randomUUID();
            UUID billId = repository.findBillIdByPayment(paymentId);
            repository.insertReceipt(receiptId, paymentId, billId, request.channel(), request.recipientAddress());
        } else {
            repository.updateReceiptDelivery(paymentId, request.channel(), request.recipientAddress());
        }
        SA.irms.billing.application.view.BillingViews.ReceiptView receiptView = billingReadService.findReceipt(paymentId);
        deliveryAdapter.deliver(new ReceiptDeliveryAdapter.DeliveryRequest(
                paymentId,
                receiptView.billId(),
                receiptView.id(),
                request.channel(),
                receiptView.recipientAddress()
        ));
        outboxPublisher.publish(new ReceiptGeneratedEvent(receiptView.id().toString(), java.util.Map.of(
                "receiptId", receiptView.id().toString(),
                "paymentId", paymentId.toString(),
                "billId", receiptView.billId().toString(),
                "channel", request.channel(),
                "recipientAddress", receiptView.recipientAddress() == null ? "" : receiptView.recipientAddress()
        )), "receipt-" + receiptView.id(), null);
        return receiptView;
    }

    public byte[] buildReceiptDocument(UUID paymentId) {
        SA.irms.billing.application.view.BillingViews.ReceiptView receipt = billingReadService.findReceipt(paymentId);
        PaymentRecord payment = billingReadService.loadPaymentRecord(paymentId);
        SA.irms.billing.application.view.BillingViews.BillView bill = billingReadService.findBill(receipt.billId());
        BranchView branch = identityPolicyPort.findDefaultBranch();

        List<String> lines = new java.util.ArrayList<>();
        lines.add(branch.name());
        lines.add("Receipt " + receipt.id());
        lines.add("Bill " + bill.id());
        lines.add("Table #" + bill.tableNumber());
        lines.add("Issued " + receipt.issuedAt());
        lines.add("");
        for (SA.irms.billing.application.view.BillingViews.LineView item : bill.items()) {
            lines.add(item.quantity() + " x " + item.name() + "  $" + item.total());
        }
        lines.add("");
        lines.add("Subtotal: $" + bill.subtotal());
        lines.add("Tax: $" + bill.taxAmount());
        lines.add("Service fee: $" + bill.serviceFee());
        lines.add("Tip: $" + bill.tipAmount());
        lines.add("Discount: $" + bill.discount());
        lines.add("Total: $" + bill.total());
        lines.add("Paid by: " + payment.method());
        lines.add("Payment amount: $" + payment.amount());
        return receiptPdfRenderer.render(lines);
    }

    private ReceiptDeliveryAdapter resolveReceiptDeliveryAdapter(String channel) {
        if (channel == null || channel.isBlank()) {
            throw new ConflictException("A receipt delivery channel is required.");
        }
        return receiptDeliveryAdapters.stream()
                .filter(adapter -> adapter.supports(channel))
                .findFirst()
                .orElseThrow(() -> new ConflictException("Unsupported receipt delivery channel."));
    }
}
