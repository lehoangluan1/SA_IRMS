package SA.irms.ordering.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

final class JdbcRecipeCommandRepository {
    private final JdbcClient jdbcClient;

    JdbcRecipeCommandRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    void createRecipe(UUID recipeId, UUID menuItemId, int version) {
        jdbcClient.sql("""
                        insert into recipes (recipe_id, menu_item_id, version, yield_unit, is_active)
                        values (:recipeId, :menuItemId, :version, 'portion', true)
                        """)
                .param("recipeId", recipeId)
                .param("menuItemId", menuItemId)
                .param("version", version)
                .update();
    }

    Optional<UUID> findInventoryItemIdByName(String ingredientName) {
        return jdbcClient.sql("""
                        select inventory_item_id
                        from inventory_items
                        where lower(name) = lower(:ingredientName)
                        """)
                .param("ingredientName", ingredientName)
                .query(UUID.class)
                .optional();
    }

    void createRecipeLine(UUID recipeId, UUID inventoryItemId, BigDecimal requiredQty) {
        jdbcClient.sql("""
                        insert into recipe_lines (recipe_line_id, recipe_id, inventory_item_id, required_qty, waste_factor)
                        values (:recipeLineId, :recipeId, :inventoryItemId, :requiredQty, 0)
                        """)
                .param("recipeLineId", UUID.randomUUID())
                .param("recipeId", recipeId)
                .param("inventoryItemId", inventoryItemId)
                .param("requiredQty", requiredQty)
                .update();
    }
}
