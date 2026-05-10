package SA.irms.inventory.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class LowStockSeverityPolicy {
    public String alertSeverity(BigDecimal current, BigDecimal minimum) {
        return current.compareTo(minimum.divide(BigDecimal.valueOf(2))) <= 0 ? "critical" : "high";
    }

    public String notificationPriority(BigDecimal current, BigDecimal minimum) {
        return current.compareTo(minimum.divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP)) <= 0 ? "critical" : "high";
    }
}
