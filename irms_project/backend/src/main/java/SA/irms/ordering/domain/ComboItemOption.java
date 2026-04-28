package SA.irms.ordering.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record ComboItemOption(
        UUID optionId,
        UUID menuItemId,
        String menuItemName,
        String station,
        BigDecimal extraPrice,
        boolean active
) {
}
