package SA.irms.ordering.infrastructure.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.ordering.application.command.MenuCommands;

final class JdbcMenuItemCommandRepository {
    private final JdbcClient jdbcClient;

    JdbcMenuItemCommandRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    void createMenuItem(UUID menuItemId, UUID categoryId, MenuCommands.MenuItemUpsert request, String allergensJson) {
        jdbcClient.sql("""
                        insert into menu_items (menu_item_id, category_id, name, description, base_price, station, availability,
                                                sale_status, preparation_time_min, allergens_json)
                        values (:menuItemId, :categoryId, :name, :description, :basePrice, :station, 'available', 'active',
                                :preparationTimeMin, cast(:allergensJson as jsonb))
                        """)
                .param("menuItemId", menuItemId)
                .param("categoryId", categoryId)
                .param("name", request.name())
                .param("description", request.description())
                .param("basePrice", request.price())
                .param("station", request.station())
                .param("preparationTimeMin", request.preparationTimeMin() == null ? 10 : request.preparationTimeMin())
                .param("allergensJson", allergensJson)
                .update();
    }

    boolean updateMenuItem(UUID menuItemId, UUID categoryId, MenuCommands.MenuItemUpsert request, String allergensJson) {
        return jdbcClient.sql("""
                        update menu_items
                        set category_id = :categoryId, name = :name, description = :description, base_price = :basePrice,
                            station = :station, preparation_time_min = :preparationTimeMin,
                            allergens_json = cast(:allergensJson as jsonb), updated_at = now()
                        where menu_item_id = :menuItemId
                        """)
                .param("menuItemId", menuItemId)
                .param("categoryId", categoryId)
                .param("name", request.name())
                .param("description", request.description())
                .param("basePrice", request.price())
                .param("station", request.station())
                .param("preparationTimeMin", request.preparationTimeMin() == null ? 10 : request.preparationTimeMin())
                .param("allergensJson", allergensJson)
                .update() == 1;
    }

    void retireActiveRecipes(UUID menuItemId) {
        jdbcClient.sql("update recipes set is_active = false, retired_at = now() where menu_item_id = :menuItemId and is_active = true")
                .param("menuItemId", menuItemId)
                .update();
    }

    void updateMenuItemAvailability(UUID menuItemId, String availability, String saleStatus) {
        jdbcClient.sql("""
                        update menu_items
                        set availability = :availability, sale_status = :saleStatus, updated_at = now()
                        where menu_item_id = :menuItemId
                        """)
                .param("availability", availability)
                .param("saleStatus", saleStatus)
                .param("menuItemId", menuItemId)
                .update();
    }

    boolean deleteMenuItem(UUID menuItemId) {
        jdbcClient.sql("delete from recipes where menu_item_id = :menuItemId")
                .param("menuItemId", menuItemId)
                .update();
        return jdbcClient.sql("delete from menu_items where menu_item_id = :menuItemId")
                .param("menuItemId", menuItemId)
                .update() == 1;
    }
}
