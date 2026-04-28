package SA.irms.billing.domain;

import java.math.BigDecimal;
import java.util.List;

/** Domain input for bill split policies. It intentionally contains no application-layer DTOs. */
public record BillSplitInput(
        String method,
        Integer splitCount,
        List<BigDecimal> amounts,
        List<BigDecimal> tipAmounts
) {
    public BillSplitInput {
        amounts = amounts == null ? List.of() : List.copyOf(amounts);
        tipAmounts = tipAmounts == null ? List.of() : List.copyOf(tipAmounts);
    }
}
