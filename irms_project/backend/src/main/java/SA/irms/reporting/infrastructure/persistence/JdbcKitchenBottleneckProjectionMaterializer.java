package SA.irms.reporting.infrastructure.persistence;

import java.time.LocalDate;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;

final class JdbcKitchenBottleneckProjectionMaterializer extends ReportingProjectionJdbcSupport {
    JdbcKitchenBottleneckProjectionMaterializer(JdbcClient jdbcClient) {
        super(jdbcClient);
    }

    void materialize(EventEnvelope envelope) {
        if (!ServiceEventTypes.KITCHEN_DISH_STATUS_CHANGED.equals(envelope.metadata().eventType())
                && !ServiceEventTypes.KITCHEN_TICKET_CREATED.equals(envelope.metadata().eventType())) {
            return;
        }
        LocalDate businessDate = businessDate(envelope);
        String station = String.valueOf(envelope.payload().getOrDefault("station", "unknown"));
        long delayedCount = "delayed".equalsIgnoreCase(String.valueOf(envelope.payload().getOrDefault("status", ""))) ? 1 : 0;
        java.math.BigDecimal delayMinutes = decimal(envelope.payload().getOrDefault("delayMinutes", 0));
        jdbcClient.sql("""
                        insert into reporting_kitchen_bottleneck_projection (
                            business_date, station, delayed_item_count, average_delay_minutes
                        ) values (
                            :businessDate, :station, :delayedItemCount, :averageDelayMinutes
                        )
                        on conflict (business_date, station) do update
                        set delayed_item_count = reporting_kitchen_bottleneck_projection.delayed_item_count + excluded.delayed_item_count,
                            average_delay_minutes = ((reporting_kitchen_bottleneck_projection.average_delay_minutes * greatest(reporting_kitchen_bottleneck_projection.delayed_item_count, 1)) + excluded.average_delay_minutes)
                                                    / greatest(reporting_kitchen_bottleneck_projection.delayed_item_count + excluded.delayed_item_count, 1),
                            updated_at = now()
                        """)
                .param("businessDate", businessDate)
                .param("station", station)
                .param("delayedItemCount", delayedCount)
                .param("averageDelayMinutes", delayMinutes)
                .update();
    }
}
