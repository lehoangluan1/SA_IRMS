package SA.irms.ordering.application;

import java.util.List;
import java.util.UUID;

public record DraftOrderItemRow(UUID orderItemId, UUID menuItemId, int quantity, String status, List<UUID> modifierOptionIds) {
}
