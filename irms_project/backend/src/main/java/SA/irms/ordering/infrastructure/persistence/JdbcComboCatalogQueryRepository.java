package SA.irms.ordering.infrastructure.persistence;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.common.error.NotFoundException;
import SA.irms.ordering.application.port.out.OrderQueryRepository;
import SA.irms.ordering.application.view.MenuViews;

final class JdbcComboCatalogQueryRepository {
    private final JdbcClient jdbcClient;

    JdbcComboCatalogQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    List<MenuViews.ComboView> loadMenuCombos() {
        return jdbcClient.sql("""
                        select combo_id, name, description, combo_price, active
                        from menu_combos
                        order by name
                        """)
                .query((rs, rowNum) -> new MenuViews.ComboView(
                        rs.getObject("combo_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBigDecimal("combo_price"),
                        rs.getBoolean("active"),
                        loadComboGroups(rs.getObject("combo_id", UUID.class))
                ))
                .list();
    }

    MenuViews.ComboView loadMenuCombo(UUID comboId) {
        return jdbcClient.sql("""
                        select combo_id, name, description, combo_price, active
                        from menu_combos
                        where combo_id = :comboId
                        """)
                .param("comboId", comboId)
                .query((rs, rowNum) -> new MenuViews.ComboView(
                        rs.getObject("combo_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBigDecimal("combo_price"),
                        rs.getBoolean("active"),
                        loadComboGroups(rs.getObject("combo_id", UUID.class))
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Combo was not found."));
    }

    OrderQueryRepository.ResolvedComboRow loadResolvedCombo(UUID comboId) {
        return jdbcClient.sql("""
                        select combo_id, name, description, combo_price, active
                        from menu_combos
                        where combo_id = :comboId
                        """)
                .param("comboId", comboId)
                .query((rs, rowNum) -> new OrderQueryRepository.ResolvedComboRow(
                        rs.getObject("combo_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBigDecimal("combo_price"),
                        rs.getBoolean("active"),
                        jdbcClient.sql("""
                                        select combo_group_id, name, min_selections, max_selections, required
                                        from menu_combo_groups
                                        where combo_id = :comboId
                                        order by display_order, name
                                        """)
                                .param("comboId", comboId)
                                .query((groupRs, groupRowNum) -> new OrderQueryRepository.ResolvedComboGroupRow(
                                        groupRs.getObject("combo_group_id", UUID.class),
                                        groupRs.getString("name"),
                                        groupRs.getInt("min_selections"),
                                        groupRs.getInt("max_selections"),
                                        groupRs.getBoolean("required")
                                ))
                                .list()
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Combo was not found."));
    }

    List<OrderQueryRepository.ResolvedComboOptionRow> loadResolvedComboOptions(UUID comboId, UUID comboGroupId, List<UUID> selectedOptionIds) {
        if (selectedOptionIds == null || selectedOptionIds.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql("""
                        select o.combo_group_id,
                               o.combo_option_id,
                               o.menu_item_id,
                               m.name as menu_item_name,
                               m.station,
                               o.extra_price,
                               o.active
                        from menu_combo_options o
                        join menu_combo_groups g on g.combo_group_id = o.combo_group_id
                        join menu_combos c on c.combo_id = g.combo_id
                        join menu_items m on m.menu_item_id = o.menu_item_id
                        where c.combo_id = :comboId
                          and o.combo_group_id = :comboGroupId
                          and o.combo_option_id in (:selectedOptionIds)
                          and o.active = true
                        order by m.name
                        """)
                .param("comboId", comboId)
                .param("comboGroupId", comboGroupId)
                .param("selectedOptionIds", selectedOptionIds)
                .query((rs, rowNum) -> new OrderQueryRepository.ResolvedComboOptionRow(
                        rs.getObject("combo_group_id", UUID.class),
                        rs.getObject("combo_option_id", UUID.class),
                        rs.getObject("menu_item_id", UUID.class),
                        rs.getString("menu_item_name"),
                        rs.getString("station"),
                        rs.getBigDecimal("extra_price"),
                        rs.getBoolean("active")
                ))
                .list();
    }

    List<OrderQueryRepository.OrderComboSelectionPayloadRow> loadOrderComboSelections(UUID orderId) {
        return jdbcClient.sql("""
                        select combo_selection_id, combo_id, combo_name, quantity,
                               coalesce(allergy_notes, '') as allergy_notes,
                               coalesce(special_instructions, '') as special_instructions
                        from order_combo_selections
                        where order_id = :orderId
                        order by created_at
                        """)
                .param("orderId", orderId)
                .query((rs, rowNum) -> {
                    UUID comboSelectionId = rs.getObject("combo_selection_id", UUID.class);
                    return new OrderQueryRepository.OrderComboSelectionPayloadRow(
                            rs.getObject("combo_id", UUID.class),
                            rs.getString("combo_name"),
                            rs.getInt("quantity"),
                            rs.getString("allergy_notes"),
                            rs.getString("special_instructions"),
                            jdbcClient.sql("""
                                            select combo_group_id, array_agg(combo_option_id order by created_at) as selected_option_ids
                                            from order_combo_selection_items
                                            where combo_selection_id = :comboSelectionId
                                            group by combo_group_id
                                            """)
                                    .param("comboSelectionId", comboSelectionId)
                                    .query((itemRs, itemRowNum) -> new OrderQueryRepository.OrderComboGroupSelectionPayloadRow(
                                            itemRs.getObject("combo_group_id", UUID.class),
                                            Arrays.asList((UUID[]) itemRs.getArray("selected_option_ids").getArray())
                                    ))
                                    .list()
                    );
                })
                .list();
    }

    private List<MenuViews.ComboGroupView> loadComboGroups(UUID comboId) {
        return jdbcClient.sql("""
                        select combo_group_id, name, min_selections, max_selections, required
                        from menu_combo_groups
                        where combo_id = :comboId
                        order by display_order, name
                        """)
                .param("comboId", comboId)
                .query((rs, rowNum) -> {
                    UUID comboGroupId = rs.getObject("combo_group_id", UUID.class);
                    return new MenuViews.ComboGroupView(
                            comboGroupId,
                            rs.getString("name"),
                            rs.getInt("min_selections"),
                            rs.getInt("max_selections"),
                            rs.getBoolean("required"),
                            loadComboOptions(comboGroupId)
                    );
                })
                .list();
    }

    private List<MenuViews.ComboOptionView> loadComboOptions(UUID comboGroupId) {
        return jdbcClient.sql("""
                        select o.combo_option_id, o.menu_item_id, m.name as menu_item_name, m.station, o.extra_price, o.active
                        from menu_combo_options o
                        join menu_items m on m.menu_item_id = o.menu_item_id
                        where o.combo_group_id = :comboGroupId
                        order by m.name
                        """)
                .param("comboGroupId", comboGroupId)
                .query((rs, rowNum) -> new MenuViews.ComboOptionView(
                        rs.getObject("combo_option_id", UUID.class),
                        rs.getObject("menu_item_id", UUID.class),
                        rs.getString("menu_item_name"),
                        rs.getString("station"),
                        rs.getBigDecimal("extra_price"),
                        rs.getBoolean("active")
                ))
                .list();
    }
}
