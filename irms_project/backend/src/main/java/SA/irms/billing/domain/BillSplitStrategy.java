package SA.irms.billing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface BillSplitStrategy {
    String method();

    List<SplitPlan> split(SplitContext context, BillSplitInput request);

    default Map<UUID, BigDecimal> proportionalAllocations(List<SplitLine> lines, BigDecimal amount, BigDecimal grandTotal) {
        Map<UUID, BigDecimal> allocations = new LinkedHashMap<>();
        if (lines.isEmpty() || grandTotal.compareTo(BigDecimal.ZERO) == 0) {
            return allocations;
        }
        BigDecimal allocated = BigDecimal.ZERO;
        for (int index = 0; index < lines.size(); index++) {
            SplitLine line = lines.get(index);
            BigDecimal lineAmount = index == lines.size() - 1
                    ? amount.subtract(allocated)
                    : amount.multiply(line.total()).divide(grandTotal, 2, RoundingMode.HALF_UP);
            allocations.put(line.lineId(), lineAmount);
            allocated = allocated.add(lineAmount);
        }
        return allocations;
    }

    record SplitContext(
            BigDecimal grandTotal,
            Integer guestCount,
            List<SplitLine> lines
    ) {
    }

    record SplitLine(UUID lineId, String label, BigDecimal total) {
    }

    record SplitPlan(String label, BigDecimal amount, Map<UUID, BigDecimal> allocations) {
    }
}
