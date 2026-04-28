package SA.irms.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.inventory.application.port.out.InventoryIngredientRepository;

@Repository
public class JdbcInventoryIngredientRepository implements InventoryIngredientRepository {
    private final JdbcClient jdbcClient;

    public JdbcInventoryIngredientRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void createIngredient(UUID inventoryItemId, UUID branchId, String name, String unit, BigDecimal onHand,
                                 BigDecimal minimumStock, BigDecimal maximumStock, BigDecimal costPerUnit, String category) {
        jdbcClient.sql("""
                        insert into inventory_items (inventory_item_id, branch_id, name, unit, on_hand, threshold,
                                                     minimum_stock, maximum_stock, cost_per_unit, category)
                        values (:inventoryItemId, :branchId, :name, :unit, :onHand, :minimumStock,
                                :minimumStock, :maximumStock, :costPerUnit, :category)
                        """)
                .param("inventoryItemId", inventoryItemId)
                .param("branchId", branchId)
                .param("name", name)
                .param("unit", unit)
                .param("onHand", onHand)
                .param("minimumStock", minimumStock)
                .param("maximumStock", maximumStock)
                .param("costPerUnit", costPerUnit)
                .param("category", category)
                .update();
    }

    @Override
    public void updateIngredient(UUID inventoryItemId, String name, String unit, BigDecimal onHand,
                                 BigDecimal minimumStock, BigDecimal maximumStock, BigDecimal costPerUnit, String category) {
        jdbcClient.sql("""
                        update inventory_items
                        set name = :name, unit = :unit, on_hand = :onHand, threshold = :minimumStock,
                            minimum_stock = :minimumStock, maximum_stock = :maximumStock, cost_per_unit = :costPerUnit,
                            category = :category, updated_at = now()
                        where inventory_item_id = :inventoryItemId
                        """)
                .param("name", name)
                .param("unit", unit)
                .param("onHand", onHand)
                .param("minimumStock", minimumStock)
                .param("maximumStock", maximumStock)
                .param("costPerUnit", costPerUnit)
                .param("category", category)
                .param("inventoryItemId", inventoryItemId)
                .update();
    }

    @Override
    public void upsertReorderRule(UUID inventoryItemId, BigDecimal threshold, BigDecimal targetQty) {
        long existing = jdbcClient.sql("select count(*) from reorder_rules where inventory_item_id = :inventoryItemId")
                .param("inventoryItemId", inventoryItemId)
                .query(Long.class)
                .single();
        if (existing == 0) {
            jdbcClient.sql("""
                            insert into reorder_rules (rule_id, inventory_item_id, threshold, target_qty, lead_time_days, is_active)
                            values (:ruleId, :inventoryItemId, :threshold, :targetQty, 2, true)
                            """)
                    .param("ruleId", UUID.randomUUID())
                    .param("inventoryItemId", inventoryItemId)
                    .param("threshold", threshold)
                    .param("targetQty", targetQty)
                    .update();
            return;
        }
        jdbcClient.sql("""
                        update reorder_rules
                        set threshold = :threshold, target_qty = :targetQty, updated_at = now()
                        where inventory_item_id = :inventoryItemId
                        """)
                .param("threshold", threshold)
                .param("targetQty", targetQty)
                .param("inventoryItemId", inventoryItemId)
                .update();
    }

    @Override
    public long countDependentRecipes(UUID inventoryItemId) {
        return jdbcClient.sql("select count(*) from recipe_lines where inventory_item_id = :inventoryItemId")
                .param("inventoryItemId", inventoryItemId)
                .query(Long.class)
                .single();
    }

    @Override
    public int deleteIngredient(UUID inventoryItemId) {
        jdbcClient.sql("delete from reorder_rules where inventory_item_id = :inventoryItemId")
                .param("inventoryItemId", inventoryItemId)
                .update();
        return jdbcClient.sql("delete from inventory_items where inventory_item_id = :inventoryItemId")
                .param("inventoryItemId", inventoryItemId)
                .update();
    }
}
