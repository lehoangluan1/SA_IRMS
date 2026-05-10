package SA.irms.ordering.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.ordering.application.port.out.OrderQueryRepository;
import SA.irms.ordering.application.view.OrderViews;

final class JdbcModifierQueryRepository {
    private final JdbcClient jdbcClient;

    JdbcModifierQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    int countModifiers(UUID menuItemId) {
        return jdbcClient.sql("select count(*) from modifier_groups where menu_item_id = :menuItemId")
                .param("menuItemId", menuItemId)
                .query(Integer.class)
                .single();
    }

    List<String> loadIngredientDisplay(UUID menuItemId) {
        return jdbcClient.sql("""
                        select i.name || ':' || cast(rl.required_qty as text) as ingredient
                        from recipes r
                        join recipe_lines rl on rl.recipe_id = r.recipe_id
                        join inventory_items i on i.inventory_item_id = rl.inventory_item_id
                        where r.menu_item_id = :menuItemId
                          and r.is_active = true
                        order by i.name
                        """)
                .param("menuItemId", menuItemId)
                .query(String.class)
                .list();
    }

    List<OrderQueryRepository.ModifierGroupConfigRow> loadModifierGroupConfigs(UUID menuItemId) {
        return jdbcClient.sql("""
                        select group_id, name, min_select, max_select, required, multi_select
                        from modifier_groups
                        where menu_item_id = :menuItemId
                        order by display_order, name
                        """)
                .param("menuItemId", menuItemId)
                .query((rs, rowNum) -> new OrderQueryRepository.ModifierGroupConfigRow(
                        rs.getObject("group_id", UUID.class),
                        rs.getString("name"),
                        rs.getInt("min_select"),
                        rs.getInt("max_select"),
                        rs.getBoolean("required"),
                        rs.getBoolean("multi_select")
                ))
                .list();
    }

    List<OrderQueryRepository.ResolvedModifierOptionRow> loadResolvedModifierOptions(UUID menuItemId, List<UUID> requestedOptionIds) {
        if (requestedOptionIds == null || requestedOptionIds.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql("""
                        select mo.option_id, mo.group_id, mo.name, mo.extra_price
                        from modifier_options mo
                        join modifier_groups mg on mg.group_id = mo.group_id
                        where mg.menu_item_id = :menuItemId
                          and mo.option_id in (:optionIds)
                          and mo.active = true
                        order by mo.display_order, mo.name
                        """)
                .param("menuItemId", menuItemId)
                .param("optionIds", requestedOptionIds)
                .query((rs, rowNum) -> new OrderQueryRepository.ResolvedModifierOptionRow(
                        rs.getObject("option_id", UUID.class),
                        rs.getObject("group_id", UUID.class),
                        rs.getString("name"),
                        rs.getBigDecimal("extra_price")
                ))
                .list();
    }

    List<UUID> loadModifierOptionIds(UUID orderItemId) {
        return jdbcClient.sql("""
                        select modifier_option_id
                        from order_item_modifiers
                        where order_item_id = :orderItemId
                          and modifier_option_id is not null
                        order by created_at
                        """)
                .param("orderItemId", orderItemId)
                .query(UUID.class)
                .list();
    }

    List<OrderViews.ModifierGroupView> loadModifierGroups(UUID menuItemId) {
        return jdbcClient.sql("""
                        select group_id, name, min_select, max_select, required, multi_select
                        from modifier_groups
                        where menu_item_id = :menuItemId
                        order by display_order, name
                        """)
                .param("menuItemId", menuItemId)
                .query((rs, rowNum) -> {
                    UUID groupId = rs.getObject("group_id", UUID.class);
                    return new OrderViews.ModifierGroupView(
                            groupId,
                            rs.getString("name"),
                            rs.getInt("min_select"),
                            rs.getInt("max_select"),
                            rs.getBoolean("required"),
                            rs.getBoolean("multi_select"),
                            loadModifierOptions(groupId)
                    );
                })
                .list();
    }

    List<OrderViews.ModifierOptionView> loadModifierOptions(UUID groupId) {
        return jdbcClient.sql("""
                        select option_id, name, extra_price, active
                        from modifier_options
                        where group_id = :groupId
                        order by display_order, name
                        """)
                .param("groupId", groupId)
                .query((rs, rowNum) -> new OrderViews.ModifierOptionView(
                        rs.getObject("option_id", UUID.class),
                        rs.getString("name"),
                        rs.getBigDecimal("extra_price"),
                        rs.getBoolean("active")
                ))
                .list();
    }
}
