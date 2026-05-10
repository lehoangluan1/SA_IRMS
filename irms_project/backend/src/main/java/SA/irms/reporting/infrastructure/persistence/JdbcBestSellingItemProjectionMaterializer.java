package SA.irms.reporting.infrastructure.persistence;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;

final class JdbcBestSellingItemProjectionMaterializer extends ReportingProjectionJdbcSupport {
    JdbcBestSellingItemProjectionMaterializer(JdbcClient jdbcClient) {
        super(jdbcClient);
    }

    void materialize(EventEnvelope envelope) {
        if (!ServiceEventTypes.ORDER_CONFIRMED.equals(envelope.metadata().eventType())) {
            return;
        }
        UUID orderId = UUID.fromString(envelope.metadata().aggregateId());
        LocalDate businessDate = businessDate(envelope);
        jdbcClient.sql("""
                        insert into reporting_best_selling_item_projection (
                            business_date, menu_item_id, item_name, quantity_sold, revenue_total
                        )
                        select :businessDate,
                               oi.menu_item_id,
                               oi.snapshot_name,
                               sum(oi.quantity),
                               sum(oi.unit_price * oi.quantity)
                        from order_items oi
                        where oi.order_id = :orderId
                        group by oi.menu_item_id, oi.snapshot_name
                        on conflict (business_date, menu_item_id) do update
                        set quantity_sold = reporting_best_selling_item_projection.quantity_sold + excluded.quantity_sold,
                            revenue_total = reporting_best_selling_item_projection.revenue_total + excluded.revenue_total,
                            updated_at = now()
                        """)
                .param("businessDate", businessDate)
                .param("orderId", orderId)
                .update();
    }
}
