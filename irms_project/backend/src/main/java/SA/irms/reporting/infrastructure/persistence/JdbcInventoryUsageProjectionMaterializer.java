package SA.irms.reporting.infrastructure.persistence;

import java.time.LocalDate;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;

final class JdbcInventoryUsageProjectionMaterializer extends ReportingProjectionJdbcSupport {
    JdbcInventoryUsageProjectionMaterializer(JdbcClient jdbcClient) {
        super(jdbcClient);
    }

    void materialize(EventEnvelope envelope) {
        if (!ServiceEventTypes.INVENTORY_STOCK_CHANGED.equals(envelope.metadata().eventType())
                && !ServiceEventTypes.LOW_STOCK_DETECTED.equals(envelope.metadata().eventType())) {
            return;
        }
        LocalDate businessDate = businessDate(envelope);
        String inventoryItemId = stringValue(envelope.payload().get("inventoryItemId"), envelope.metadata().aggregateId());
        java.math.BigDecimal delta = decimal(envelope.payload().getOrDefault("delta", 0));
        String ingredientName = jdbcClient.sql("select name from inventory_items where inventory_item_id::text = :inventoryItemId")
                .param("inventoryItemId", inventoryItemId)
                .query(String.class)
                .optional().orElse("inventory-item-" + inventoryItemId);
        jdbcClient.sql("""
                        insert into reporting_inventory_usage_projection (
                            business_date, ingredient_id, ingredient_name, quantity_used, waste_quantity
                        ) values (
                            :businessDate, :ingredientId, :ingredientName, :quantityUsed, :wasteQuantity
                        )
                        on conflict (business_date, ingredient_id) do update
                        set quantity_used = reporting_inventory_usage_projection.quantity_used + excluded.quantity_used,
                            waste_quantity = reporting_inventory_usage_projection.waste_quantity + excluded.waste_quantity,
                            updated_at = now()
                        """)
                .param("businessDate", businessDate)
                .param("ingredientId", inventoryItemId)
                .param("ingredientName", ingredientName)
                .param("quantityUsed", delta.signum() < 0 ? delta.abs() : java.math.BigDecimal.ZERO)
                .param("wasteQuantity", "waste".equalsIgnoreCase(stringValue(envelope.payload().get("reason"), "")) ? delta.abs() : java.math.BigDecimal.ZERO)
                .update();
    }
}
