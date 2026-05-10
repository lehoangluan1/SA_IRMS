package SA.irms.inventory.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.inventory.application.port.out.InventoryLowStockAlertRepository;

@Repository
public class JdbcInventoryLowStockAlertRepository implements InventoryLowStockAlertRepository {
    private final JdbcClient jdbcClient;

    public JdbcInventoryLowStockAlertRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<LowStockSnapshot> findSnapshot(UUID inventoryItemId) {
        return jdbcClient.sql("""
                        select inventory_item_id, name, on_hand, minimum_stock
                        from inventory_items
                        where inventory_item_id = :inventoryItemId
                        """)
                .param("inventoryItemId", inventoryItemId)
                .query((rs, rowNum) -> new LowStockSnapshot(
                        rs.getObject("inventory_item_id", UUID.class),
                        rs.getString("name"),
                        rs.getBigDecimal("on_hand"),
                        rs.getBigDecimal("minimum_stock")
                ))
                .optional();
    }

    @Override
    public boolean hasOpenAlert(UUID inventoryItemId) {
        return jdbcClient.sql("""
                        select count(*)
                        from low_stock_alerts
                        where inventory_item_id = :inventoryItemId
                          and status = 'open'
                        """)
                .param("inventoryItemId", inventoryItemId)
                .query(Long.class)
                .single() > 0;
    }

    @Override
    public UUID createOpenAlert(UUID inventoryItemId, String severity) {
        UUID alertId = UUID.randomUUID();
        jdbcClient.sql("""
                        insert into low_stock_alerts (
                            alert_id,
                            inventory_item_id,
                            severity,
                            status,
                            created_at
                        ) values (
                            :alertId,
                            :inventoryItemId,
                            :severity,
                            'open',
                            now()
                        )
                        """)
                .param("alertId", alertId)
                .param("inventoryItemId", inventoryItemId)
                .param("severity", severity)
                .update();
        return alertId;
    }

    @Override
    public void acknowledgeAlert(UUID alertId, UUID actorUserId) {
        jdbcClient.sql("""
                        update low_stock_alerts
                        set status = 'acknowledged',
                            acknowledged_by = :actorUserId,
                            acknowledged_at = now(),
                            updated_at = now()
                        where alert_id = :alertId
                        """)
                .param("actorUserId", actorUserId)
                .param("alertId", alertId)
                .update();
    }
}
