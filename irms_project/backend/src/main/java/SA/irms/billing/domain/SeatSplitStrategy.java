package SA.irms.billing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import SA.irms.common.error.ConflictException;

public class SeatSplitStrategy implements BillSplitStrategy {
    @Override
    public String method() {
        return "seat";
    }

    @Override
    public List<SplitPlan> split(SplitContext context, BillSplitInput request) {
        if (context.guestCount() == null || context.guestCount() < 1) {
            throw new ConflictException("Guest count is required for seat-based split.");
        }
        List<SplitPlan> plans = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < context.guestCount(); index++) {
            BigDecimal amount = index == context.guestCount() - 1
                    ? context.grandTotal().subtract(allocated)
                    : context.grandTotal().divide(BigDecimal.valueOf(context.guestCount()), 2, RoundingMode.HALF_UP);
            allocated = allocated.add(amount);
            plans.add(new SplitPlan(
                    "Seat " + (index + 1),
                    amount,
                    proportionalAllocations(context.lines(), amount, context.grandTotal())
            ));
        }
        return plans;
    }
}
