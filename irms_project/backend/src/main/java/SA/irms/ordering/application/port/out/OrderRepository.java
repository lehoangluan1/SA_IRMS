package SA.irms.ordering.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    int nextCategoryDisplayOrder();
    void createCategory(UUID categoryId, String name, int displayOrder);
    boolean updateCategory(UUID categoryId, String name);
    long countMenuItemsInCategory(UUID categoryId);
    boolean deleteCategory(UUID categoryId);
    Optional<UUID> findCategoryIdByName(String categoryName);
    void createMenuItem(UUID menuItemId, UUID categoryId, SA.irms.ordering.application.command.MenuCommands.MenuItemUpsert request, String allergensJson);
    boolean updateMenuItem(UUID menuItemId, UUID categoryId, SA.irms.ordering.application.command.MenuCommands.MenuItemUpsert request, String allergensJson);
    void retireActiveRecipes(UUID menuItemId);
    void updateMenuItemAvailability(UUID menuItemId, String availability, String saleStatus);
    boolean deleteMenuItem(UUID menuItemId);
    void createPromotion(UUID promotionId, String code, String name, String discountType, BigDecimal discountValue, java.sql.Timestamp effectiveTo);
    boolean updatePromotion(UUID promotionId, String code, String name, String discountType, BigDecimal discountValue, java.sql.Timestamp effectiveTo);
    boolean deletePromotion(UUID promotionId);
    Optional<PromotionRow> findActivePromotionByCode(String code);
    Optional<BigDecimal> findBillSubtotal(UUID billId);
    void applyPromotionToBill(UUID billId, UUID promotionId, String code, BigDecimal discount);
    void createRecipe(UUID recipeId, UUID menuItemId, int version);
    Optional<UUID> findInventoryItemIdByName(String ingredientName);
    void createRecipeLine(UUID recipeId, UUID inventoryItemId, BigDecimal requiredQty);
    void replaceModifierSelections(UUID orderItemId, List<ModifierSelectionRow> selectedOptions);
    void insertModifierSelections(UUID orderItemId, List<ModifierSelectionRow> selectedOptions);
    Optional<String> findSessionStatus(UUID sessionId);
    void createOrder(UUID orderId, UUID sessionId, UUID serverUserId, String status, String specialInstructions, String correlationId, Instant createdAt, Instant confirmedAt);
    void createOrderItem(UUID orderId, UUID orderItemId, UUID menuItemId, String snapshotName, int quantity, BigDecimal unitPrice,
                         String specialInstruction, String allergyNotes, String lineStatus, boolean draft, boolean sendLater);
    void updateOrderSnapshot(UUID orderId, BigDecimal subtotal, Instant pricedAt);
    void updateDraftOrderItem(UUID orderItemId, String snapshotName, BigDecimal unitPrice);
    void confirmDraftOrder(UUID orderId, Instant confirmedAt);
    void updateOrderLineStatus(UUID orderId, UUID orderItemId, String lineStatus);
    void updateOrderStatus(UUID orderId, String status);
    Optional<OrderItemServedState> findServedState(UUID orderItemId);
    void markOrderItemServed(UUID orderItemId);
    void markOrderItemDelayed(UUID orderItemId);
    void markOrderItemSentToKitchen(UUID orderItemId);
    void cancelOrderItem(UUID orderItemId, String reason);
    int cancelActiveOrderItems(UUID orderId, String reason);
    void markOrderCancelled(UUID orderId);
    Optional<String> findOrderStatus(UUID orderId);
    void createMenuCombo(UUID comboId, String name, String description, BigDecimal comboPrice, boolean active);
    void updateMenuCombo(UUID comboId, String name, String description, BigDecimal comboPrice, boolean active);
    void replaceMenuComboGroups(UUID comboId, List<SA.irms.ordering.application.command.MenuCommands.ComboGroupUpsert> groups);
    void createOrderComboSelection(UUID orderId, UUID comboSelectionId, UUID comboId, String comboName, int quantity,
                                   BigDecimal comboPrice, String allergyNotes, String specialInstructions);
    void createOrderComboSelectionItem(UUID comboSelectionId, UUID comboGroupId, UUID comboOptionId, UUID menuItemId,
                                       String menuItemName, String station, int quantity, BigDecimal allocatedUnitPrice);

    record PromotionRow(UUID id, String type, BigDecimal value) {}
    record ModifierSelectionRow(UUID optionId, String name, BigDecimal extraPrice) {}
    record OrderItemServedState(String status, Instant servedAt) {}
}
