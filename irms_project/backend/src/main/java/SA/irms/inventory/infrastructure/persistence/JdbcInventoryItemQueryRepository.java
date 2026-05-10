package SA.irms.inventory.infrastructure.persistence;

import SA.irms.inventory.application.view.InventoryViews.IngredientView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcInventoryItemQueryRepository {
    private final JdbcClient jdbcClient;
    private final InventoryMenuImpactLookup menuImpactLookup;
    private final InventoryRelativeTimeFormatter relativeTimeFormatter;

    JdbcInventoryItemQueryRepository(
            JdbcClient jdbcClient,
            InventoryMenuImpactLookup menuImpactLookup,
            InventoryRelativeTimeFormatter relativeTimeFormatter
    ) {
        this.jdbcClient = jdbcClient;
        this.menuImpactLookup = menuImpactLookup;
        this.relativeTimeFormatter = relativeTimeFormatter;
    }

    List<IngredientView> loadItems() {
        List<IngredientRow> rows = jdbcClient.sql("""
                        select inventory_item_id, name, unit, on_hand, minimum_stock, maximum_stock,
                               cost_per_unit, category, last_restocked_at
                        from inventory_items
                        order by name
                        """)
                .query((rs, rowNum) -> new IngredientRow(
                        rs.getObject("inventory_item_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("unit"),
                        rs.getBigDecimal("on_hand"),
                        rs.getBigDecimal("minimum_stock"),
                        rs.getBigDecimal("maximum_stock"),
                        rs.getBigDecimal("cost_per_unit"),
                        rs.getString("category"),
                        rs.getTimestamp("last_restocked_at") == null ? null : rs.getTimestamp("last_restocked_at").toInstant()
                ))
                .list();
        Map<UUID, List<String>> affectedMenuItems = menuImpactLookup.loadAffectedMenuItemsByInventoryItemIds(
                rows.stream().map(IngredientRow::inventoryItemId).toList()
        );
        return rows.stream()
                .map(row -> toView(row, affectedMenuItems))
                .toList();
    }

    Optional<IngredientView> findIngredient(UUID inventoryItemId) {
        return jdbcClient.sql("""
                        select inventory_item_id, name, unit, on_hand, minimum_stock, maximum_stock,
                               cost_per_unit, category, last_restocked_at
                        from inventory_items
                        where inventory_item_id = :inventoryItemId
                        """)
                .param("inventoryItemId", inventoryItemId)
                .query((rs, rowNum) -> new IngredientRow(
                        rs.getObject("inventory_item_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("unit"),
                        rs.getBigDecimal("on_hand"),
                        rs.getBigDecimal("minimum_stock"),
                        rs.getBigDecimal("maximum_stock"),
                        rs.getBigDecimal("cost_per_unit"),
                        rs.getString("category"),
                        rs.getTimestamp("last_restocked_at") == null ? null : rs.getTimestamp("last_restocked_at").toInstant()
                ))
                .optional()
                .map(row -> toView(
                        row,
                        menuImpactLookup.loadAffectedMenuItemsByInventoryItemIds(List.of(row.inventoryItemId()))
                ));
    }

    private IngredientView toView(IngredientRow row, Map<UUID, List<String>> affectedMenuItems) {
        return new IngredientView(
                row.inventoryItemId(),
                row.name(),
                row.unit(),
                row.onHand(),
                row.minimumStock(),
                row.maximumStock(),
                row.costPerUnit(),
                row.category(),
                row.lastRestockedAt() == null ? "Never" : relativeTimeFormatter.format(row.lastRestockedAt()),
                affectedMenuItems.getOrDefault(row.inventoryItemId(), List.of())
        );
    }

    private record IngredientRow(
            UUID inventoryItemId,
            String name,
            String unit,
            BigDecimal onHand,
            BigDecimal minimumStock,
            BigDecimal maximumStock,
            BigDecimal costPerUnit,
            String category,
            Instant lastRestockedAt
    ) {
    }
}
