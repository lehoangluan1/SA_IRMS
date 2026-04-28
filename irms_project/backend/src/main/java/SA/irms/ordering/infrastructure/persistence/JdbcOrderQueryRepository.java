package SA.irms.ordering.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.ordering.application.port.out.OrderQueryRepository;
import SA.irms.ordering.application.support.MenuJsonSupport;
import SA.irms.ordering.application.view.MenuViews;
import SA.irms.ordering.application.view.OrderViews;

@Repository
public class JdbcOrderQueryRepository implements OrderQueryRepository {
    private final JdbcMenuCatalogQueryRepository menuCatalogQueryRepository;
    private final JdbcModifierQueryRepository modifierQueryRepository;
    private final JdbcTableSessionQueryRepository tableSessionQueryRepository;
    private final JdbcOrderedItemQueryRepository orderedItemQueryRepository;
    private final JdbcDraftOrderQueryRepository draftOrderQueryRepository;
    private final JdbcComboCatalogQueryRepository comboCatalogQueryRepository;
    private final JdbcOrderRoutingStatusQueryRepository orderRoutingStatusQueryRepository;

    public JdbcOrderQueryRepository(JdbcClient jdbcClient, MenuJsonSupport menuJsonSupport) {
        this.modifierQueryRepository = new JdbcModifierQueryRepository(jdbcClient);
        this.comboCatalogQueryRepository = new JdbcComboCatalogQueryRepository(jdbcClient);
        this.menuCatalogQueryRepository = new JdbcMenuCatalogQueryRepository(
                jdbcClient,
                menuJsonSupport,
                modifierQueryRepository,
                comboCatalogQueryRepository
        );
        this.tableSessionQueryRepository = new JdbcTableSessionQueryRepository(jdbcClient, menuJsonSupport, modifierQueryRepository);
        this.orderedItemQueryRepository = new JdbcOrderedItemQueryRepository(jdbcClient);
        this.draftOrderQueryRepository = new JdbcDraftOrderQueryRepository(jdbcClient, modifierQueryRepository);
        this.orderRoutingStatusQueryRepository = new JdbcOrderRoutingStatusQueryRepository(jdbcClient);
    }

    @Override
    public List<MenuViews.CategoryView> loadCategories() {
        return menuCatalogQueryRepository.loadCategories();
    }

    @Override
    public List<MenuViews.MenuItemView> loadMenuItems() {
        return menuCatalogQueryRepository.loadMenuItems();
    }

    @Override
    public List<MenuViews.PromotionView> loadPromotions() {
        return menuCatalogQueryRepository.loadPromotions();
    }

    @Override
    public List<MenuViews.ComboView> loadMenuCombos() {
        return comboCatalogQueryRepository.loadMenuCombos();
    }

    @Override
    public MenuViews.ComboView loadMenuCombo(UUID comboId) {
        return comboCatalogQueryRepository.loadMenuCombo(comboId);
    }

    @Override
    public int countModifiers(UUID menuItemId) {
        return modifierQueryRepository.countModifiers(menuItemId);
    }

    @Override
    public List<String> loadIngredientDisplay(UUID menuItemId) {
        return modifierQueryRepository.loadIngredientDisplay(menuItemId);
    }

    @Override
    public Optional<ResolvedMenuItemRow> findResolvedMenuItem(UUID menuItemId) {
        return menuCatalogQueryRepository.findResolvedMenuItem(menuItemId);
    }

    @Override
    public List<ModifierGroupConfigRow> loadModifierGroupConfigs(UUID menuItemId) {
        return modifierQueryRepository.loadModifierGroupConfigs(menuItemId);
    }

    @Override
    public List<ResolvedModifierOptionRow> loadResolvedModifierOptions(UUID menuItemId, List<UUID> requestedOptionIds) {
        return modifierQueryRepository.loadResolvedModifierOptions(menuItemId, requestedOptionIds);
    }

    @Override
    public List<UUID> loadModifierOptionIds(UUID orderItemId) {
        return modifierQueryRepository.loadModifierOptionIds(orderItemId);
    }

    @Override
    public List<OrderViews.ModifierGroupView> loadModifierGroups(UUID menuItemId) {
        return modifierQueryRepository.loadModifierGroups(menuItemId);
    }

    @Override
    public List<OrderViews.ModifierOptionView> loadModifierOptions(UUID groupId) {
        return modifierQueryRepository.loadModifierOptions(groupId);
    }

    @Override
    public List<OrderViews.TableSessionView> loadTableSessions() {
        return tableSessionQueryRepository.loadTableSessions();
    }

    @Override
    public List<OrderViews.MenuItemView> loadMenuItemsForOrdering() {
        return tableSessionQueryRepository.loadMenuItemsForOrdering();
    }

    @Override
    public List<OrderViews.OrderedItemView> loadOrderedItems(UUID sessionId) {
        return orderedItemQueryRepository.loadOrderedItems(sessionId);
    }

    @Override
    public ResolvedComboRow loadResolvedCombo(UUID comboId) {
        return comboCatalogQueryRepository.loadResolvedCombo(comboId);
    }

    @Override
    public List<ResolvedComboOptionRow> loadResolvedComboOptions(UUID comboId, UUID comboGroupId, List<UUID> selectedOptionIds) {
        return comboCatalogQueryRepository.loadResolvedComboOptions(comboId, comboGroupId, selectedOptionIds);
    }

    @Override
    public DraftOrderRow loadDraftOrder(UUID orderId) {
        return draftOrderQueryRepository.loadDraftOrder(orderId);
    }

    @Override
    public List<DraftOrderItemRow> loadDraftOrderItems(UUID orderId) {
        return draftOrderQueryRepository.loadDraftOrderItems(orderId);
    }

    @Override
    public List<OrderComboSelectionPayloadRow> loadOrderComboSelections(UUID orderId) {
        return comboCatalogQueryRepository.loadOrderComboSelections(orderId);
    }

    @Override
    public Optional<OrderItemRoutingRow> findOrderItemRouting(UUID orderItemId) {
        return orderRoutingStatusQueryRepository.findOrderItemRouting(orderItemId);
    }

    @Override
    public OrderViews.OrderedItemView loadOrderedItemView(UUID orderItemId) {
        return orderedItemQueryRepository.loadOrderedItemView(orderItemId);
    }

    @Override
    public Optional<OrderStateRow> findOrderForItem(UUID orderItemId) {
        return orderedItemQueryRepository.findOrderForItem(orderItemId);
    }

    @Override
    public List<String> loadOrderItemStatuses(UUID orderId) {
        return orderedItemQueryRepository.loadOrderItemStatuses(orderId);
    }
}
