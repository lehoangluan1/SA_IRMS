package SA.irms.ordering.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record Combo(
        UUID comboId,
        String name,
        String description,
        BigDecimal comboPrice,
        boolean active,
        List<ComboGroup> groups
) {
}
