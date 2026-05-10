package SA.irms.inventory.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.inventory.application.port.out.InventoryStockLevelQueryPort;

@Repository
public class JdbcInventoryStockLevelAdapter implements InventoryStockLevelQueryPort {
    private final JdbcClient jdbcClient;

    public JdbcInventoryStockLevelAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<StockLevel> findStockLevel(UUID inventoryItemId) {
        return jdbcClient.sql("""
                        select inventory_item_id, on_hand as quantity_on_hand, threshold as low_stock_threshold
                        from inventory_items
                        where inventory_item_id = :inventoryItemId
                        """)
                .param("inventoryItemId", inventoryItemId)
                .query((rs, rowNum) -> new StockLevel(
                        rs.getObject("inventory_item_id", UUID.class),
                        rs.getBigDecimal("quantity_on_hand"),
                        rs.getBigDecimal("low_stock_threshold")
                ))
                .optional();
    }
}
