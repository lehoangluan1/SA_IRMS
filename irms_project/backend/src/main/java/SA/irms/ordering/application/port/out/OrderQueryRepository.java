package SA.irms.ordering.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderQueryRepository {
    List<SA.irms.ordering.application.view.MenuViews.CategoryView> loadCategories();

    List<SA.irms.ordering.application.view.MenuViews.MenuItemView> loadMenuItems();

    List<SA.irms.ordering.application.view.MenuViews.PromotionView> loadPromotions();

    List<SA.irms.ordering.application.view.MenuViews.ComboView> loadMenuCombos();

    SA.irms.ordering.application.view.MenuViews.ComboView loadMenuCombo(UUID comboId);

    int countModifiers(UUID menuItemId);

    List<String> loadIngredientDisplay(UUID menuItemId);

    Optional<ResolvedMenuItemRow> findResolvedMenuItem(UUID menuItemId);

    List<ModifierGroupConfigRow> loadModifierGroupConfigs(UUID menuItemId);

    List<ResolvedModifierOptionRow> loadResolvedModifierOptions(UUID menuItemId, List<UUID> requestedOptionIds);

    List<UUID> loadModifierOptionIds(UUID orderItemId);

    List<SA.irms.ordering.application.view.OrderViews.ModifierGroupView> loadModifierGroups(UUID menuItemId);

    List<SA.irms.ordering.application.view.OrderViews.ModifierOptionView> loadModifierOptions(UUID groupId);

    List<SA.irms.ordering.application.view.OrderViews.TableSessionView> loadTableSessions();

    List<SA.irms.ordering.application.view.OrderViews.MenuItemView> loadMenuItemsForOrdering();

    List<SA.irms.ordering.application.view.OrderViews.OrderedItemView> loadOrderedItems(UUID sessionId);

    ResolvedComboRow loadResolvedCombo(UUID comboId);

    List<ResolvedComboOptionRow> loadResolvedComboOptions(UUID comboId, UUID comboGroupId, List<UUID> selectedOptionIds);

    DraftOrderRow loadDraftOrder(UUID orderId);

    List<DraftOrderItemRow> loadDraftOrderItems(UUID orderId);

    List<OrderComboSelectionPayloadRow> loadOrderComboSelections(UUID orderId);

    Optional<OrderItemRoutingRow> findOrderItemRouting(UUID orderItemId);

    SA.irms.ordering.application.view.OrderViews.OrderedItemView loadOrderedItemView(UUID orderItemId);

    Optional<OrderStateRow> findOrderForItem(UUID orderItemId);

    List<String> loadOrderItemStatuses(UUID orderId);

    record ResolvedMenuItemRow(UUID id, String name, BigDecimal basePrice, String station, String availability) {}
    record ResolvedModifierOptionRow(UUID optionId, UUID groupId, String name, BigDecimal extraPrice) {}
    record ModifierGroupConfigRow(UUID id, String name, int minSelect, int maxSelect, boolean required, boolean multiSelect) {}
    record DraftOrderRow(UUID orderId, UUID tableSessionId, String status, String specialInstructions) {}
    record DraftOrderItemRow(UUID orderItemId, UUID menuItemId, int quantity, String status, List<UUID> modifierOptionIds) {}
    record OrderComboSelectionPayloadRow(UUID comboId, String comboName, int quantity, String allergyNotes, String specialInstructions, List<OrderComboGroupSelectionPayloadRow> groups) {}
    record OrderComboGroupSelectionPayloadRow(UUID comboGroupId, List<UUID> selectedOptionIds) {}
    record OrderItemRoutingRow(UUID orderId, String orderStatus, int quantity, String status, String station) {}
    record OrderStateRow(UUID orderId, String status) {}
    record ResolvedComboRow(UUID comboId, String name, String description, BigDecimal comboPrice, boolean active,
                            List<ResolvedComboGroupRow> groups) {}
    record ResolvedComboGroupRow(UUID comboGroupId, String name, int minSelections, int maxSelections, boolean required) {}
    record ResolvedComboOptionRow(UUID comboGroupId, UUID comboOptionId, UUID menuItemId, String menuItemName,
                                  String station, BigDecimal extraPrice, boolean active) {}
}
