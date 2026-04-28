package SA.irms.billing.domain;

import java.util.List;
import java.util.Map;

public class ItemSplitStrategy implements BillSplitStrategy {
    @Override
    public String method() {
        return "item";
    }

    @Override
    public List<SplitPlan> split(SplitContext context, BillSplitInput request) {
        return context.lines().stream()
                .map(line -> new SplitPlan(line.label(), line.total(), Map.of(line.lineId(), line.total())))
                .toList();
    }
}
