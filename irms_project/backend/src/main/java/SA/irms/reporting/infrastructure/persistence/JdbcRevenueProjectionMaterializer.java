package SA.irms.reporting.infrastructure.persistence;

import java.time.LocalDate;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;

final class JdbcRevenueProjectionMaterializer extends ReportingProjectionJdbcSupport {
    JdbcRevenueProjectionMaterializer(JdbcClient jdbcClient) {
        super(jdbcClient);
    }

    void materialize(EventEnvelope envelope) {
        if (!ServiceEventTypes.PAYMENT_COMPLETED.equals(envelope.metadata().eventType())
                && !ServiceEventTypes.REFUND_ISSUED.equals(envelope.metadata().eventType())) {
            return;
        }
        LocalDate businessDate = businessDate(envelope);
        String paymentMethod = String.valueOf(envelope.payload().getOrDefault("method", "unknown"));
        java.math.BigDecimal amount = decimal(envelope.payload().get("amount"));
        java.math.BigDecimal refunds = ServiceEventTypes.REFUND_ISSUED.equals(envelope.metadata().eventType()) ? amount : java.math.BigDecimal.ZERO;
        java.math.BigDecimal gross = ServiceEventTypes.PAYMENT_COMPLETED.equals(envelope.metadata().eventType()) ? amount : java.math.BigDecimal.ZERO;
        jdbcClient.sql("""
                        insert into reporting_revenue_projection (
                            business_date, payment_method, gross_revenue, discounts, refunds, net_revenue
                        ) values (
                            :businessDate, :paymentMethod, :grossRevenue, 0, :refunds, :netRevenue
                        )
                        on conflict (business_date, payment_method) do update
                        set gross_revenue = reporting_revenue_projection.gross_revenue + excluded.gross_revenue,
                            refunds = reporting_revenue_projection.refunds + excluded.refunds,
                            net_revenue = reporting_revenue_projection.net_revenue + excluded.net_revenue,
                            updated_at = now()
                        """)
                .param("businessDate", businessDate)
                .param("paymentMethod", paymentMethod)
                .param("grossRevenue", gross)
                .param("refunds", refunds)
                .param("netRevenue", gross.subtract(refunds))
                .update();
    }
}
