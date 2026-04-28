package SA.irms.kitchen.application.view;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class KitchenViews {
    private KitchenViews() {}

    public record KitchenOverview(List<String> stations, List<TicketView> tickets) {}
    public record TicketView(UUID id, UUID orderId, int tableNumber, String serverName, String priority, String status,
                             Instant createdAt, List<TicketItemView> items) {}
    public record TicketItemView(UUID id, UUID orderItemId, String name, int quantity, List<String> modifiers,
                                 String allergyNotes, String specialInstructions, String status, String station, String holdReason) {}
}
