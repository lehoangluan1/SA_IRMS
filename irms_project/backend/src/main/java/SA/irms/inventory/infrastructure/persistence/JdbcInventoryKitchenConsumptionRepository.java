package SA.irms.inventory.infrastructure.persistence;

import SA.irms.inventory.application.port.out.InventoryKitchenConsumptionRepository;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcInventoryKitchenConsumptionRepository implements InventoryKitchenConsumptionRepository {
    private final JdbcClient jdbcClient;

    JdbcInventoryKitchenConsumptionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<RecipeUsage> loadRecipeUsages(UUID orderItemId) {
        return jdbcClient.sql("""
                        select i.inventory_item_id,
                               rl.required_qty,
                               rl.waste_factor
                        from order_items oi
                        join menu_items mi on mi.menu_item_id = oi.menu_item_id
                        join recipes r on r.menu_item_id = mi.menu_item_id and r.is_active = true
                        join recipe_lines rl on rl.recipe_id = r.recipe_id
                        join inventory_items i on i.inventory_item_id = rl.inventory_item_id
                        where oi.order_item_id = :orderItemId
                        """)
                .param("orderItemId", orderItemId)
                .query((rs, rowNum) -> new RecipeUsage(
                        rs.getObject("inventory_item_id", UUID.class),
                        rs.getBigDecimal("required_qty"),
                        rs.getBigDecimal("waste_factor")
                ))
                .list();
    }

    @Override
    public boolean hasRecordedKitchenStartConsumption(UUID inventoryItemId, UUID orderItemId) {
        long existingTransactions = jdbcClient.sql("""
                        select count(*)
                        from stock_transactions
                        where inventory_item_id = :inventoryItemId
                          and source_ref = :orderItemId
                          and source_type = 'order_item_cooking_start'
                          and reason = 'consumption'
                        """)
                .param("inventoryItemId", inventoryItemId)
                .param("orderItemId", orderItemId)
                .query(Long.class)
                .single();
        return existingTransactions > 0;
    }
}
