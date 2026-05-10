package SA.irms.reporting.infrastructure.persistence;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;

final class JdbcComboSalesProjectionMaterializer extends ReportingProjectionJdbcSupport {
    JdbcComboSalesProjectionMaterializer(JdbcClient jdbcClient) {
        super(jdbcClient);
    }

    void materialize(EventEnvelope envelope) {
        if (!ServiceEventTypes.ORDER_CONFIRMED.equals(envelope.metadata().eventType())) {
            return;
        }
        UUID orderId = UUID.fromString(envelope.metadata().aggregateId());
        LocalDate businessDate = businessDate(envelope);
        jdbcClient.sql("""
                        insert into reporting_combo_sales_projection (
                            business_date, combo_id, combo_name, quantity_sold, revenue_total
                        )
                        select :businessDate,
                               combo_id::text,
                               combo_name,
                               sum(quantity),
                               sum(combo_price * quantity)
                        from order_combo_selections
                        where order_id = :orderId
                        group by combo_id, combo_name
                        on conflict (business_date, combo_id) do update
                        set quantity_sold = reporting_combo_sales_projection.quantity_sold + excluded.quantity_sold,
                            revenue_total = reporting_combo_sales_projection.revenue_total + excluded.revenue_total,
                            updated_at = now()
                        """)
                .param("businessDate", businessDate)
                .param("orderId", orderId)
                .update();
    }
}
