package SA.irms.billing.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import SA.irms.common.error.ConflictException;

public class AmountSplitStrategy implements BillSplitStrategy {
    @Override
    public String method() {
        return "amount";
    }

    @Override
    public List<SplitPlan> split(SplitContext context, BillSplitInput request) {
        if (request.amounts() == null || request.amounts().isEmpty()) {
            throw new ConflictException("Amounts are required for amount-based split.");
        }
        BigDecimal totalAmounts = request.amounts().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalAmounts.compareTo(context.grandTotal()) != 0) {
            throw new ConflictException("Split amounts must equal the bill total.");
        }
        List<SplitPlan> plans = new ArrayList<>();
        for (int index = 0; index < request.amounts().size(); index++) {
            BigDecimal amount = request.amounts().get(index);
            plans.add(new SplitPlan(
                    "Person " + (index + 1),
                    amount,
                    proportionalAllocations(context.lines(), amount, context.grandTotal())
            ));
        }
        return plans;
    }
}
