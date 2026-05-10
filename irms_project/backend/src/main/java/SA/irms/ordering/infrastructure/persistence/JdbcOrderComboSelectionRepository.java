package SA.irms.ordering.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.ordering.application.command.MenuCommands;

final class JdbcOrderComboSelectionRepository {
    private final JdbcClient jdbcClient;

    JdbcOrderComboSelectionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    void createMenuCombo(UUID comboId, String name, String description, BigDecimal comboPrice, boolean active) {
        jdbcClient.sql("""
                        insert into menu_combos (combo_id, name, description, combo_price, active)
                        values (:comboId, :name, :description, :comboPrice, :active)
                        """)
                .param("comboId", comboId)
                .param("name", name)
                .param("description", description)
                .param("comboPrice", comboPrice)
                .param("active", active)
                .update();
    }

    void updateMenuCombo(UUID comboId, String name, String description, BigDecimal comboPrice, boolean active) {
        jdbcClient.sql("""
                        update menu_combos
                        set name = :name,
                            description = :description,
                            combo_price = :comboPrice,
                            active = :active,
                            updated_at = now()
                        where combo_id = :comboId
                        """)
                .param("comboId", comboId)
                .param("name", name)
                .param("description", description)
                .param("comboPrice", comboPrice)
                .param("active", active)
                .update();
    }

    void replaceMenuComboGroups(UUID comboId, List<MenuCommands.ComboGroupUpsert> groups) {
        jdbcClient.sql("delete from menu_combo_options where combo_group_id in (select combo_group_id from menu_combo_groups where combo_id = :comboId)")
                .param("comboId", comboId)
                .update();
        jdbcClient.sql("delete from menu_combo_groups where combo_id = :comboId")
                .param("comboId", comboId)
                .update();
        int displayOrder = 1;
        for (MenuCommands.ComboGroupUpsert group : groups) {
            UUID comboGroupId = UUID.randomUUID();
            jdbcClient.sql("""
                            insert into menu_combo_groups (combo_group_id, combo_id, name, min_selections, max_selections, required, display_order)
                            values (:comboGroupId, :comboId, :name, :minSelections, :maxSelections, :required, :displayOrder)
                            """)
                    .param("comboGroupId", comboGroupId)
                    .param("comboId", comboId)
                    .param("name", group.name())
                    .param("minSelections", group.minSelections() == null ? 0 : group.minSelections())
                    .param("maxSelections", group.maxSelections() == null ? 1 : group.maxSelections())
                    .param("required", Boolean.TRUE.equals(group.required()))
                    .param("displayOrder", displayOrder++)
                    .update();
            if (group.options() == null) {
                continue;
            }
            for (MenuCommands.ComboOptionUpsert option : group.options()) {
                jdbcClient.sql("""
                                insert into menu_combo_options (combo_option_id, combo_group_id, menu_item_id, extra_price, active)
                                values (:comboOptionId, :comboGroupId, :menuItemId, :extraPrice, :active)
                                """)
                        .param("comboOptionId", UUID.randomUUID())
                        .param("comboGroupId", comboGroupId)
                        .param("menuItemId", option.menuItemId())
                        .param("extraPrice", option.extraPrice())
                        .param("active", option.active() == null || option.active())
                        .update();
            }
        }
    }

    void createOrderComboSelection(UUID orderId, UUID comboSelectionId, UUID comboId, String comboName, int quantity,
                                   BigDecimal comboPrice, String allergyNotes, String specialInstructions) {
        jdbcClient.sql("""
                        insert into order_combo_selections (combo_selection_id, order_id, combo_id, combo_name, quantity, combo_price, allergy_notes, special_instructions)
                        values (:comboSelectionId, :orderId, :comboId, :comboName, :quantity, :comboPrice, :allergyNotes, :specialInstructions)
                        """)
                .param("comboSelectionId", comboSelectionId)
                .param("orderId", orderId)
                .param("comboId", comboId)
                .param("comboName", comboName)
                .param("quantity", quantity)
                .param("comboPrice", comboPrice)
                .param("allergyNotes", allergyNotes)
                .param("specialInstructions", specialInstructions)
                .update();
    }

    void createOrderComboSelectionItem(UUID comboSelectionId, UUID comboGroupId, UUID comboOptionId, UUID menuItemId,
                                       String menuItemName, String station, int quantity, BigDecimal allocatedUnitPrice) {
        jdbcClient.sql("""
                        insert into order_combo_selection_items (
                            combo_selection_item_id, combo_selection_id, combo_group_id, combo_option_id,
                            menu_item_id, menu_item_name, station, quantity, allocated_unit_price
                        ) values (
                            :comboSelectionItemId, :comboSelectionId, :comboGroupId, :comboOptionId,
                            :menuItemId, :menuItemName, :station, :quantity, :allocatedUnitPrice
                        )
                        """)
                .param("comboSelectionItemId", UUID.randomUUID())
                .param("comboSelectionId", comboSelectionId)
                .param("comboGroupId", comboGroupId)
                .param("comboOptionId", comboOptionId)
                .param("menuItemId", menuItemId)
                .param("menuItemName", menuItemName)
                .param("station", station)
                .param("quantity", quantity)
                .param("allocatedUnitPrice", allocatedUnitPrice)
                .update();
    }
}
