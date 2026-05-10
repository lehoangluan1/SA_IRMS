package SA.irms.ordering.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.ordering.application.port.out.OrderQueryRepository;
import SA.irms.ordering.application.support.MenuJsonSupport;
import SA.irms.ordering.application.view.MenuViews;

final class JdbcMenuCatalogQueryRepository {
    private final JdbcClient jdbcClient;
    private final MenuJsonSupport menuJsonSupport;
    private final JdbcModifierQueryRepository modifierQueryRepository;
    private final JdbcComboCatalogQueryRepository comboCatalogQueryRepository;

    JdbcMenuCatalogQueryRepository(
            JdbcClient jdbcClient,
            MenuJsonSupport menuJsonSupport,
            JdbcModifierQueryRepository modifierQueryRepository,
            JdbcComboCatalogQueryRepository comboCatalogQueryRepository
    ) {
        this.jdbcClient = jdbcClient;
        this.menuJsonSupport = menuJsonSupport;
        this.modifierQueryRepository = modifierQueryRepository;
        this.comboCatalogQueryRepository = comboCatalogQueryRepository;
    }

    List<MenuViews.CategoryView> loadCategories() {
        return jdbcClient.sql("""
                        select c.category_id, c.name, c.is_active, count(m.menu_item_id) as item_count
                        from menu_categories c
                        left join menu_items m on m.category_id = c.category_id
                        group by c.category_id, c.name, c.is_active, c.display_order
                        order by c.display_order, c.name
                        """)
                .query((rs, rowNum) -> new MenuViews.CategoryView(
                        rs.getObject("category_id", UUID.class),
                        rs.getString("name"),
                        rs.getInt("item_count"),
                        rs.getBoolean("is_active")
                ))
                .list();
    }

    List<MenuViews.MenuItemView> loadMenuItems() {
        return jdbcClient.sql("""
                        select m.menu_item_id, m.name, m.description, c.name as category_name, m.base_price,
                               m.station, m.availability, m.sale_status, m.allergens_json::text as allergens_json,
                               coalesce(max(r.version), 1) as version
                        from menu_items m
                        join menu_categories c on c.category_id = m.category_id
                        left join recipes r on r.menu_item_id = m.menu_item_id
                        group by m.menu_item_id, m.name, m.description, c.name, m.base_price, m.station, m.availability, m.sale_status, m.allergens_json
                        order by c.name, m.name
                        """)
                .query((rs, rowNum) -> {
                    UUID menuItemId = rs.getObject("menu_item_id", UUID.class);
                    return new MenuViews.MenuItemView(
                            menuItemId,
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("category_name"),
                            rs.getBigDecimal("base_price"),
                            rs.getString("station"),
                            "available".equals(rs.getString("availability")),
                            menuJsonSupport.parseJsonArray(rs.getString("allergens_json")),
                            modifierQueryRepository.countModifiers(menuItemId),
                            rs.getInt("version"),
                            modifierQueryRepository.loadIngredientDisplay(menuItemId)
                    );
                })
                .list();
    }

    List<MenuViews.PromotionView> loadPromotions() {
        return jdbcClient.sql("""
                        select promotion_campaign_id, code, discount_type, discount_value, effective_to, is_active
                        from promotion_campaigns
                        order by code
                        """)
                .query((rs, rowNum) -> new MenuViews.PromotionView(
                        rs.getObject("promotion_campaign_id", UUID.class),
                        rs.getString("code"),
                        formatDiscount(rs.getString("discount_type"), rs.getBigDecimal("discount_value")),
                        rs.getTimestamp("effective_to") == null ? null : rs.getTimestamp("effective_to").toLocalDateTime().toLocalDate().toString(),
                        rs.getBoolean("is_active")
                ))
                .list();
    }

    List<MenuViews.ComboView> loadMenuCombos() {
        return comboCatalogQueryRepository.loadMenuCombos();
    }

    MenuViews.ComboView loadMenuCombo(UUID comboId) {
        return comboCatalogQueryRepository.loadMenuCombo(comboId);
    }

    Optional<OrderQueryRepository.ResolvedMenuItemRow> findResolvedMenuItem(UUID menuItemId) {
        return jdbcClient.sql("""
                        select menu_item_id, name, base_price, station, availability
                        from menu_items
                        where menu_item_id = :menuItemId
                        """)
                .param("menuItemId", menuItemId)
                .query((rs, rowNum) -> new OrderQueryRepository.ResolvedMenuItemRow(
                        rs.getObject("menu_item_id", UUID.class),
                        rs.getString("name"),
                        rs.getBigDecimal("base_price"),
                        rs.getString("station"),
                        rs.getString("availability")
                ))
                .optional();
    }

    private String formatDiscount(java.math.BigDecimal value, String type) {
        return formatDiscount(type, value);
    }

    private String formatDiscount(String type, java.math.BigDecimal value) {
        if (type == null || value == null) {
            return null;
        }
        return "percentage".equalsIgnoreCase(type)
                ? value.stripTrailingZeros().toPlainString() + "%"
                : "$" + value.stripTrailingZeros().toPlainString();
    }
}
