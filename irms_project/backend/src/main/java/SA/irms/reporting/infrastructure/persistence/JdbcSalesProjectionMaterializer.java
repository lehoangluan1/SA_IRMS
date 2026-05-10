package SA.irms.reporting.infrastructure.persistence;

import java.time.LocalDate;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.common.events.EventEnvelope;
import SA.irms.common.events.ServiceEventTypes;

final class JdbcSalesProjectionMaterializer extends ReportingProjectionJdbcSupport {
    JdbcSalesProjectionMaterializer(JdbcClient jdbcClient) {
        super(jdbcClient);
    }

    void materialize(EventEnvelope envelope) {
        LocalDate businessDate = businessDate(envelope);
        if (ServiceEventTypes.BILL_ISSUED.equals(envelope.metadata().eventType())) {
            java.math.BigDecimal gross = decimal(envelope.payload().get("grandTotal"));
            jdbcClient.sql("""
                            insert into reporting_sales_projection (business_date, order_count, bill_count, gross_sales, refund_total, net_sales)
                            values (:businessDate, 0, 1, :gross, 0, :gross)
                            on conflict (business_date) do update
                            set bill_count = reporting_sales_projection.bill_count + 1,
                                gross_sales = reporting_sales_projection.gross_sales + excluded.gross_sales,
                                net_sales = reporting_sales_projection.net_sales + excluded.net_sales,
                                updated_at = now()
                            """)
                    .param("businessDate", businessDate)
                    .param("gross", gross)
                    .update();
            return;
        }
        if (ServiceEventTypes.REFUND_ISSUED.equals(envelope.metadata().eventType())) {
            java.math.BigDecimal refund = decimal(envelope.payload().get("amount"));
            jdbcClient.sql("""
                            insert into reporting_sales_projection (business_date, order_count, bill_count, gross_sales, refund_total, net_sales)
                            values (:businessDate, 0, 0, 0, :refund, :negativeRefund)
                            on conflict (business_date) do update
                            set refund_total = reporting_sales_projection.refund_total + excluded.refund_total,
                                net_sales = reporting_sales_projection.net_sales + excluded.net_sales,
                                updated_at = now()
                            """)
                    .param("businessDate", businessDate)
                    .param("refund", refund)
                    .param("negativeRefund", refund.negate())
                    .update();
            return;
        }
        if (ServiceEventTypes.ORDER_CONFIRMED.equals(envelope.metadata().eventType())) {
            java.math.BigDecimal subtotal = decimal(envelope.payload().get("subtotal"));
            jdbcClient.sql("""
                            insert into reporting_sales_projection (business_date, order_count, bill_count, gross_sales, refund_total, net_sales)
                            values (:businessDate, 1, 0, :subtotal, 0, :subtotal)
                            on conflict (business_date) do update
                            set order_count = reporting_sales_projection.order_count + 1,
                                gross_sales = reporting_sales_projection.gross_sales + excluded.gross_sales,
                                net_sales = reporting_sales_projection.net_sales + excluded.net_sales,
                                updated_at = now()
                            """)
                    .param("businessDate", businessDate)
                    .param("subtotal", subtotal)
                    .update();
        }
    }
}
