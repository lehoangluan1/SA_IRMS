package SA.irms.ordering.application.view;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class OrderViews {
    private OrderViews() {}

    public record OrdersOverview(List<TableSessionView> sessions, List<MenuItemView> menuItems,
                                 List<SA.irms.ordering.application.view.MenuViews.ComboView> combos,
                                 List<OrderedItemView> orderedItems, UUID selectedSessionId) {}
    public record TableSessionView(UUID sessionId, int tableNumber, int guests) {}
    public record MenuItemView(UUID id, String name, BigDecimal price, String category, String station, boolean available,
                               List<String> allergens, List<ModifierGroupView> modifierGroups) {}
    public record ModifierGroupView(UUID id, String name, int minSelect, int maxSelect, boolean required, boolean multiSelect,
                                    List<ModifierOptionView> options) {}
    public record ModifierOptionView(UUID id, String name, BigDecimal extraPrice, boolean active) {}
    public record OrderedItemView(UUID id, UUID orderId, String orderStatus, String name, int quantity, BigDecimal price,
                                  String note, String status, String station) {}
}
