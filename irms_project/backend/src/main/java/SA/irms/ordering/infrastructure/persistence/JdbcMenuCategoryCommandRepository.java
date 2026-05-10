package SA.irms.ordering.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

final class JdbcMenuCategoryCommandRepository {
    private final JdbcClient jdbcClient;

    JdbcMenuCategoryCommandRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    int nextCategoryDisplayOrder() {
        return jdbcClient.sql("select coalesce(max(display_order), 0) + 1 from menu_categories")
                .query(Integer.class)
                .single();
    }

    void createCategory(UUID categoryId, String name, int displayOrder) {
        jdbcClient.sql("""
                        insert into menu_categories (category_id, name, display_order, is_active)
                        values (:categoryId, :name, :displayOrder, true)
                        """)
                .param("categoryId", categoryId)
                .param("name", name)
                .param("displayOrder", displayOrder)
                .update();
    }

    boolean updateCategory(UUID categoryId, String name) {
        return jdbcClient.sql("""
                        update menu_categories
                        set name = :name, updated_at = now()
                        where category_id = :categoryId
                        """)
                .param("name", name)
                .param("categoryId", categoryId)
                .update() == 1;
    }

    long countMenuItemsInCategory(UUID categoryId) {
        return jdbcClient.sql("select count(*) from menu_items where category_id = :categoryId")
                .param("categoryId", categoryId)
                .query(Long.class)
                .single();
    }

    boolean deleteCategory(UUID categoryId) {
        return jdbcClient.sql("delete from menu_categories where category_id = :categoryId")
                .param("categoryId", categoryId)
                .update() == 1;
    }

    Optional<UUID> findCategoryIdByName(String categoryName) {
        return jdbcClient.sql("select category_id from menu_categories where lower(name) = lower(:categoryName)")
                .param("categoryName", categoryName)
                .query(UUID.class)
                .optional();
    }
}
