package SA.irms.ordering.application.command;

import java.util.List;
import java.util.UUID;

public final class OrderCommands {
    private OrderCommands() {}

    public record CreateOrderRequest(UUID sessionId, String specialInstructions, List<OrderItemRequest> items,
                                     List<ComboSelectionRequest> comboSelections, boolean draft) {}
    public record OrderItemRequest(UUID menuItemId, int quantity, String note, List<UUID> modifierOptionIds, boolean sendLater) {}
    public record ComboSelectionRequest(UUID comboId, int quantity, String allergyNotes, String specialInstructions,
                                        List<ComboGroupSelectionRequest> groups) {}
    public record ComboGroupSelectionRequest(UUID comboGroupId, List<UUID> selectedOptionIds) {}
}
