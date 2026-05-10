package SA.irms.inventory.infrastructure.persistence;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
class InventoryMenuImpactLookup {
    private final JdbcClient jdbcClient;

    InventoryMenuImpactLookup(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    Map<UUID, List<String>> loadAffectedMenuItemsByInventoryItemIds(Collection<UUID> inventoryItemIds) {
        if (inventoryItemIds == null || inventoryItemIds.isEmpty()) {
            return Map.of();
        }
        List<UUID> distinctItemIds = inventoryItemIds.stream().distinct().toList();
        Map<UUID, List<String>> affected = new LinkedHashMap<>();
        jdbcClient.sql("""
                        select distinct rl.inventory_item_id, mi.name
                        from recipe_lines rl
                        join recipes r on r.recipe_id = rl.recipe_id
                        join menu_items mi on mi.menu_item_id = r.menu_item_id
                        where rl.inventory_item_id in (:inventoryItemIds)
                          and r.is_active = true
                        order by rl.inventory_item_id, mi.name
                        """)
                .param("inventoryItemIds", distinctItemIds)
                .query((rs, rowNum) -> new MenuImpactRow(
                        rs.getObject("inventory_item_id", UUID.class),
                        rs.getString("name")
                ))
                .list()
                .forEach(row -> affected.computeIfAbsent(row.inventoryItemId(), ignored -> new java.util.ArrayList<>())
                        .add(row.menuItemName()));
        Map<UUID, List<String>> result = new LinkedHashMap<>();
        affected.forEach((inventoryItemId, menuItems) -> result.put(inventoryItemId, List.copyOf(menuItems)));
        return Map.copyOf(result);
    }

    private record MenuImpactRow(UUID inventoryItemId, String menuItemName) {
    }
}
