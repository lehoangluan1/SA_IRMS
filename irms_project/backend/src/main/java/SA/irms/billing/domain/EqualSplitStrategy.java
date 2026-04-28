package SA.irms.billing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class EqualSplitStrategy implements BillSplitStrategy {
    @Override
    public String method() {
        return "equal";
    }

    @Override
    public List<SplitPlan> split(SplitContext context, BillSplitInput request) {
        int count = request.splitCount() == null || request.splitCount() < 2 ? 2 : request.splitCount();
        List<SplitPlan> plans = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < count; index++) {
            BigDecimal amount = index == count - 1
                    ? context.grandTotal().subtract(allocated)
                    : context.grandTotal().divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            allocated = allocated.add(amount);
            plans.add(new SplitPlan(
                    "Person " + (index + 1),
                    amount,
                    proportionalAllocations(context.lines(), amount, context.grandTotal())
            ));
        }
        return plans;
    }
}
