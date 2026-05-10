package SA.irms.billing.application;

import SA.irms.billing.application.port.out.BillSplitRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import SA.irms.billing.domain.BillSplitInput;
import SA.irms.billing.domain.BillSplitStrategy;
import SA.irms.common.error.ConflictException;

@Service
public class BillingSplitService {
    private final BillSplitRepository repository;
    private final BillingReadService billingReadService;
    private final Map<String, BillSplitStrategy> splitStrategies;

    public BillingSplitService(BillSplitRepository repository,
                               BillingReadService billingReadService,
                               List<BillSplitStrategy> splitStrategies) {
        this.repository = repository;
        this.billingReadService = billingReadService;
        this.splitStrategies = splitStrategies.stream()
                .collect(Collectors.toMap(BillSplitStrategy::method, Function.identity()));
    }

    @Transactional
    public SA.irms.billing.application.view.BillingViews.BillView splitBill(UUID billId, SA.irms.billing.application.command.BillingCommands.SplitRequest request) {
        long paidSplits = repository.countPaidSplits(billId);
        if (paidSplits > 0) {
            throw new ConflictException("Paid splits cannot be reallocated without a refund.");
        }
        repository.clearSplits(billId);

        SA.irms.billing.application.view.BillingViews.BillView bill = billingReadService.findBill(billId);
        List<SA.irms.billing.application.view.BillingViews.LineView> lines = bill.items();
        BigDecimal grandTotal = bill.total();
        Integer guests = repository.loadGuestCount(billId);
        BillSplitStrategy.SplitContext splitContext = new BillSplitStrategy.SplitContext(
                grandTotal,
                guests,
                lines.stream()
                        .map(line -> new BillSplitStrategy.SplitLine(line.lineId(), line.name(), line.total()))
                        .toList()
        );
        BillSplitInput domainInput = new BillSplitInput(request.method(), request.splitCount(), request.amounts(), request.tipAmounts());
        List<BillSplitStrategy.SplitPlan> definitions = resolveSplitStrategy(domainInput.method()).split(splitContext, domainInput);
        List<BigDecimal> splitTipAmounts = resolveSplitTipAmounts(bill, definitions, request.tipAmounts());

        for (int index = 0; index < definitions.size(); index++) {
            BillSplitStrategy.SplitPlan definition = definitions.get(index);
            UUID splitId = UUID.randomUUID();
            repository.insertSplit(splitId, billId, definition.label(), definition.amount(), splitTipAmounts.get(index));
            for (Map.Entry<UUID, BigDecimal> allocation : definition.allocations().entrySet()) {
                repository.insertSplitAllocation(
                        splitId,
                        allocation.getKey(),
                        allocation.getValue(),
                        grandTotal.compareTo(BigDecimal.ZERO) == 0
                                ? BigDecimal.ZERO
                                : allocation.getValue().divide(grandTotal, 4, RoundingMode.HALF_UP)
                );
            }
        }
        return billingReadService.findBill(billId);
    }

    private List<BigDecimal> resolveSplitTipAmounts(
            SA.irms.billing.application.view.BillingViews.BillView bill,
            List<BillSplitStrategy.SplitPlan> definitions,
            List<BigDecimal> requestedTipAmounts
    ) {
        if (requestedTipAmounts != null && !requestedTipAmounts.isEmpty()) {
            if (requestedTipAmounts.size() != definitions.size()) {
                throw new ConflictException("Tip allocation count must match the generated split count.");
            }
            BigDecimal requestedTipTotal = requestedTipAmounts.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            if (requestedTipTotal.compareTo(bill.tipAmount().setScale(2, RoundingMode.HALF_UP)) != 0) {
                throw new ConflictException("Split tip allocations must add up to the bill tip amount.");
            }
            return requestedTipAmounts;
        }
        if (bill.tipAmount().compareTo(BigDecimal.ZERO) == 0 || bill.total().compareTo(BigDecimal.ZERO) == 0) {
            return definitions.stream().map(ignored -> BigDecimal.ZERO).toList();
        }
        List<BigDecimal> allocatedTips = new java.util.ArrayList<>();
        BigDecimal runningTotal = BigDecimal.ZERO;
        for (int index = 0; index < definitions.size(); index++) {
            BigDecimal tipAmount = index == definitions.size() - 1
                    ? bill.tipAmount().subtract(runningTotal)
                    : bill.tipAmount().multiply(definitions.get(index).amount()).divide(bill.total(), 2, RoundingMode.HALF_UP);
            allocatedTips.add(tipAmount);
            runningTotal = runningTotal.add(tipAmount);
        }
        return allocatedTips;
    }

    private BillSplitStrategy resolveSplitStrategy(String method) {
        if (method == null || method.isBlank()) {
            throw new ConflictException("A split method is required.");
        }
        BillSplitStrategy strategy = splitStrategies.get(method.toLowerCase());
        if (strategy == null) {
            throw new ConflictException("Unsupported split method.");
        }
        return strategy;
    }
}
