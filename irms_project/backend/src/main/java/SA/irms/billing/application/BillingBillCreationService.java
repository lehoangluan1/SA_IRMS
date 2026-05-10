package SA.irms.billing.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.billing.application.port.out.BillIssueRepository;
import SA.irms.common.error.ConflictException;
import SA.irms.billing.application.events.BillIssuedEvent;
import SA.irms.common.identity.PolicySnapshot;
import SA.irms.common.identity.SharedIdentityPolicyPort;
import SA.irms.common.outbox.DomainEventPublisher;

@Service
public class BillingBillCreationService {
    private final BillIssueRepository repository;
    private final SharedIdentityPolicyPort identityPolicyPort;
    private final BillingReadService billingReadService;
    private final DomainEventPublisher outboxPublisher;

    public BillingBillCreationService(
            BillIssueRepository repository,
            SharedIdentityPolicyPort identityPolicyPort,
            BillingReadService billingReadService,
            DomainEventPublisher outboxPublisher
    ) {
        this.repository = repository;
        this.identityPolicyPort = identityPolicyPort;
        this.billingReadService = billingReadService;
        this.outboxPublisher = outboxPublisher;
    }

    @Transactional
    public SA.irms.billing.application.view.BillingViews.BillView createBill(UUID tableSessionId) {
        UUID existingBillId = repository.findExistingBillId(tableSessionId).orElse(null);
        if (existingBillId != null) {
            return billingReadService.findBill(existingBillId);
        }
        PolicySnapshot.TaxPolicy taxPolicy = identityPolicyPort.getPolicySnapshot().taxPolicy();
        long unreadyItems = repository.countUnreadyItems(tableSessionId);
        if (unreadyItems > 0) {
            throw new ConflictException("The bill cannot be finalized while kitchen items are still pending, cooking, blocked, or held for later service.");
        }
        List<BillIssueRepository.BillLineSource> lineSources = repository.loadBillLineSources(tableSessionId);
        if (lineSources.isEmpty()) {
            throw new ConflictException("There are no unsettled order items for this table session.");
        }
        BigDecimal subTotal = lineSources.stream()
                .map(source -> source.unitPrice().add(source.modifierTotal()).multiply(BigDecimal.valueOf(source.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taxAmount = subTotal.multiply(taxPolicy.taxRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal serviceFee = subTotal.multiply(taxPolicy.serviceFeeRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subTotal.add(taxAmount).add(serviceFee);
        UUID billId = UUID.randomUUID();
        repository.insertBill(billId, tableSessionId, subTotal, taxAmount, serviceFee, grandTotal, "bill-" + billId);
        for (BillIssueRepository.BillLineSource lineSource : lineSources) {
            repository.insertBillLine(billId, lineSource.orderItemId(), lineSource.name(), lineSource.quantity(),
                    lineSource.unitPrice().add(lineSource.modifierTotal()).multiply(BigDecimal.valueOf(lineSource.quantity())));
        }
        repository.insertFullBillSplit(billId, grandTotal);
        repository.updateTableSessionToBilling(tableSessionId);
        outboxPublisher.publish(new BillIssuedEvent(billId.toString(), Map.of(
                "billId", billId.toString(),
                "tableSessionId", tableSessionId.toString(),
                "subTotal", subTotal,
                "taxAmount", taxAmount,
                "serviceFee", serviceFee,
                "grandTotal", grandTotal
        )), "bill-" + billId, null);
        return billingReadService.findBill(billId);
    }
}
