package SA.irms.ordering.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.ordering.application.command.MenuCommands;
import SA.irms.ordering.application.port.out.OrderRepository;
import SA.irms.ordering.application.support.MenuJsonSupport;

@Repository
public class JdbcOrderRepository implements OrderRepository {
    private final JdbcMenuCategoryCommandRepository menuCategoryRepository;
    private final JdbcMenuItemCommandRepository menuItemRepository;
    private final JdbcPromotionCommandRepository promotionRepository;
    private final JdbcRecipeCommandRepository recipeRepository;
    private final JdbcOrderLifecycleRepository orderLifecycleRepository;
    private final JdbcOrderItemCommandRepository orderItemRepository;
    private final JdbcOrderComboSelectionRepository orderComboSelectionRepository;

    public JdbcOrderRepository(JdbcClient jdbcClient, MenuJsonSupport menuJsonSupport) {
        this.menuCategoryRepository = new JdbcMenuCategoryCommandRepository(jdbcClient);
        this.menuItemRepository = new JdbcMenuItemCommandRepository(jdbcClient);
        this.promotionRepository = new JdbcPromotionCommandRepository(jdbcClient);
        this.recipeRepository = new JdbcRecipeCommandRepository(jdbcClient);
        this.orderLifecycleRepository = new JdbcOrderLifecycleRepository(jdbcClient);
        this.orderItemRepository = new JdbcOrderItemCommandRepository(jdbcClient);
        this.orderComboSelectionRepository = new JdbcOrderComboSelectionRepository(jdbcClient);
    }

    @Override
    public int nextCategoryDisplayOrder() {
        return menuCategoryRepository.nextCategoryDisplayOrder();
    }

    @Override
    public void createCategory(UUID categoryId, String name, int displayOrder) {
        menuCategoryRepository.createCategory(categoryId, name, displayOrder);
    }

    @Override
    public boolean updateCategory(UUID categoryId, String name) {
        return menuCategoryRepository.updateCategory(categoryId, name);
    }

    @Override
    public long countMenuItemsInCategory(UUID categoryId) {
        return menuCategoryRepository.countMenuItemsInCategory(categoryId);
    }

    @Override
    public boolean deleteCategory(UUID categoryId) {
        return menuCategoryRepository.deleteCategory(categoryId);
    }

    @Override
    public Optional<UUID> findCategoryIdByName(String categoryName) {
        return menuCategoryRepository.findCategoryIdByName(categoryName);
    }

    @Override
    public void createMenuItem(UUID menuItemId, UUID categoryId, MenuCommands.MenuItemUpsert request, String allergensJson) {
        menuItemRepository.createMenuItem(menuItemId, categoryId, request, allergensJson);
    }

    @Override
    public boolean updateMenuItem(UUID menuItemId, UUID categoryId, MenuCommands.MenuItemUpsert request, String allergensJson) {
        return menuItemRepository.updateMenuItem(menuItemId, categoryId, request, allergensJson);
    }

    @Override
    public void retireActiveRecipes(UUID menuItemId) {
        menuItemRepository.retireActiveRecipes(menuItemId);
    }

    @Override
    public void updateMenuItemAvailability(UUID menuItemId, String availability, String saleStatus) {
        menuItemRepository.updateMenuItemAvailability(menuItemId, availability, saleStatus);
    }

    @Override
    public boolean deleteMenuItem(UUID menuItemId) {
        return menuItemRepository.deleteMenuItem(menuItemId);
    }

    @Override
    public void createPromotion(UUID promotionId, String code, String name, String discountType, BigDecimal discountValue, java.sql.Timestamp effectiveTo) {
        promotionRepository.createPromotion(promotionId, code, name, discountType, discountValue, effectiveTo);
    }

    @Override
    public boolean updatePromotion(UUID promotionId, String code, String name, String discountType, BigDecimal discountValue, java.sql.Timestamp effectiveTo) {
        return promotionRepository.updatePromotion(promotionId, code, name, discountType, discountValue, effectiveTo);
    }

    @Override
    public boolean deletePromotion(UUID promotionId) {
        return promotionRepository.deletePromotion(promotionId);
    }

    @Override
    public Optional<PromotionRow> findActivePromotionByCode(String code) {
        return promotionRepository.findActivePromotionByCode(code);
    }

    @Override
    public Optional<BigDecimal> findBillSubtotal(UUID billId) {
        return promotionRepository.findBillSubtotal(billId);
    }

    @Override
    public void applyPromotionToBill(UUID billId, UUID promotionId, String code, BigDecimal discount) {
        promotionRepository.applyPromotionToBill(billId, promotionId, code, discount);
    }

    @Override
    public void createRecipe(UUID recipeId, UUID menuItemId, int version) {
        recipeRepository.createRecipe(recipeId, menuItemId, version);
    }

    @Override
    public Optional<UUID> findInventoryItemIdByName(String ingredientName) {
        return recipeRepository.findInventoryItemIdByName(ingredientName);
    }

    @Override
    public void createRecipeLine(UUID recipeId, UUID inventoryItemId, BigDecimal requiredQty) {
        recipeRepository.createRecipeLine(recipeId, inventoryItemId, requiredQty);
    }

    @Override
    public void replaceModifierSelections(UUID orderItemId, List<ModifierSelectionRow> selectedOptions) {
        orderItemRepository.replaceModifierSelections(orderItemId, selectedOptions);
    }

    @Override
    public void insertModifierSelections(UUID orderItemId, List<ModifierSelectionRow> selectedOptions) {
        orderItemRepository.insertModifierSelections(orderItemId, selectedOptions);
    }

    @Override
    public Optional<String> findSessionStatus(UUID sessionId) {
        return orderLifecycleRepository.findSessionStatus(sessionId);
    }

    @Override
    public void createOrder(UUID orderId, UUID sessionId, UUID serverUserId, String status, String specialInstructions,
                            String correlationId, Instant createdAt, Instant confirmedAt) {
        orderLifecycleRepository.createOrder(orderId, sessionId, serverUserId, status, specialInstructions, correlationId, createdAt, confirmedAt);
    }

    @Override
    public void createOrderItem(UUID orderId, UUID orderItemId, UUID menuItemId, String snapshotName, int quantity, BigDecimal unitPrice,
                                String specialInstruction, String allergyNotes, String lineStatus, boolean draft, boolean sendLater) {
        orderItemRepository.createOrderItem(orderId, orderItemId, menuItemId, snapshotName, quantity, unitPrice,
                specialInstruction, allergyNotes, lineStatus, draft, sendLater);
    }

    @Override
    public void updateOrderSnapshot(UUID orderId, BigDecimal subtotal, Instant pricedAt) {
        orderLifecycleRepository.updateOrderSnapshot(orderId, subtotal, pricedAt);
    }

    @Override
    public void updateDraftOrderItem(UUID orderItemId, String snapshotName, BigDecimal unitPrice) {
        orderItemRepository.updateDraftOrderItem(orderItemId, snapshotName, unitPrice);
    }

    @Override
    public void confirmDraftOrder(UUID orderId, Instant confirmedAt) {
        orderLifecycleRepository.confirmDraftOrder(orderId, confirmedAt);
    }

    @Override
    public void updateOrderLineStatus(UUID orderId, UUID orderItemId, String lineStatus) {
        orderItemRepository.updateOrderLineStatus(orderId, orderItemId, lineStatus);
    }

    @Override
    public void updateOrderStatus(UUID orderId, String status) {
        orderLifecycleRepository.updateOrderStatus(orderId, status);
    }

    @Override
    public Optional<OrderItemServedState> findServedState(UUID orderItemId) {
        return orderItemRepository.findServedState(orderItemId);
    }

    @Override
    public void markOrderItemServed(UUID orderItemId) {
        orderItemRepository.markOrderItemServed(orderItemId);
    }

    @Override
    public void markOrderItemDelayed(UUID orderItemId) {
        orderItemRepository.markOrderItemDelayed(orderItemId);
    }

    @Override
    public void markOrderItemSentToKitchen(UUID orderItemId) {
        orderItemRepository.markOrderItemSentToKitchen(orderItemId);
    }

    @Override
    public void cancelOrderItem(UUID orderItemId, String reason) {
        orderItemRepository.cancelOrderItem(orderItemId, reason);
    }

    @Override
    public int cancelActiveOrderItems(UUID orderId, String reason) {
        return orderItemRepository.cancelActiveOrderItems(orderId, reason);
    }

    @Override
    public void markOrderCancelled(UUID orderId) {
        orderLifecycleRepository.markOrderCancelled(orderId);
    }

    @Override
    public Optional<String> findOrderStatus(UUID orderId) {
        return orderLifecycleRepository.findOrderStatus(orderId);
    }

    @Override
    public void createMenuCombo(UUID comboId, String name, String description, BigDecimal comboPrice, boolean active) {
        orderComboSelectionRepository.createMenuCombo(comboId, name, description, comboPrice, active);
    }

    @Override
    public void updateMenuCombo(UUID comboId, String name, String description, BigDecimal comboPrice, boolean active) {
        orderComboSelectionRepository.updateMenuCombo(comboId, name, description, comboPrice, active);
    }

    @Override
    public void replaceMenuComboGroups(UUID comboId, List<MenuCommands.ComboGroupUpsert> groups) {
        orderComboSelectionRepository.replaceMenuComboGroups(comboId, groups);
    }

    @Override
    public void createOrderComboSelection(UUID orderId, UUID comboSelectionId, UUID comboId, String comboName, int quantity,
                                          BigDecimal comboPrice, String allergyNotes, String specialInstructions) {
        orderComboSelectionRepository.createOrderComboSelection(orderId, comboSelectionId, comboId, comboName, quantity,
                comboPrice, allergyNotes, specialInstructions);
    }

    @Override
    public void createOrderComboSelectionItem(UUID comboSelectionId, UUID comboGroupId, UUID comboOptionId, UUID menuItemId,
                                              String menuItemName, String station, int quantity, BigDecimal allocatedUnitPrice) {
        orderComboSelectionRepository.createOrderComboSelectionItem(comboSelectionId, comboGroupId, comboOptionId, menuItemId,
                menuItemName, station, quantity, allocatedUnitPrice);
    }
}
