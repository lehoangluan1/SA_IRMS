package SA.irms.reporting.infrastructure.persistence;

import java.time.LocalDate;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;

final class JdbcPeakHourProjectionMaterializer extends ReportingProjectionJdbcSupport {
    JdbcPeakHourProjectionMaterializer(JdbcClient jdbcClient) {
        super(jdbcClient);
    }

    void materialize(EventEnvelope envelope) {
        if (!ServiceEventTypes.ORDER_CONFIRMED.equals(envelope.metadata().eventType())
                && !ServiceEventTypes.RESERVATION_CREATED.equals(envelope.metadata().eventType())
                && !ServiceEventTypes.RESERVATION_SEATED.equals(envelope.metadata().eventType())) {
            return;
        }
        LocalDate businessDate = businessDate(envelope);
        int hour = envelope.metadata().occurredAt().atZone(java.time.ZoneOffset.UTC).getHour();
        java.math.BigDecimal revenue = decimal(envelope.payload().get("subtotal"));
        jdbcClient.sql("""
                        insert into reporting_peak_hour_projection (business_date, hour_of_day, order_count, revenue_total)
                        values (:businessDate, :hourOfDay, 1, :revenueTotal)
                        on conflict (business_date, hour_of_day) do update
                        set order_count = reporting_peak_hour_projection.order_count + 1,
                            revenue_total = reporting_peak_hour_projection.revenue_total + excluded.revenue_total,
                            updated_at = now()
                        """)
                .param("businessDate", businessDate)
                .param("hourOfDay", hour)
                .param("revenueTotal", revenue)
                .update();
    }
}
