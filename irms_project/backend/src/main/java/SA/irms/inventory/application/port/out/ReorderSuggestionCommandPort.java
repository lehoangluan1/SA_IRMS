package SA.irms.inventory.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface ReorderSuggestionCommandPort {
    void upsertSuggestion(UUID inventoryItemId, BigDecimal threshold, BigDecimal targetQuantity, int leadTimeDays);
}
