package SA.irms.ordering.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ComboPricingPolicy {
    public BigDecimal componentUnitPrice(BigDecimal comboBasePrice, BigDecimal optionExtraPrice, int totalSelectedUnits) {
        if (totalSelectedUnits <= 0) {
            return comboBasePrice.add(optionExtraPrice == null ? BigDecimal.ZERO : optionExtraPrice);
        }
        BigDecimal total = comboBasePrice.add(optionExtraPrice == null ? BigDecimal.ZERO : optionExtraPrice);
        return total.divide(BigDecimal.valueOf(totalSelectedUnits), 2, RoundingMode.HALF_UP);
    }
}
