package SA.irms.reporting.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OperationsDashboardQueryRepository {
    List<AlertRow> loadOpenLowStockAlerts();

    List<AuditAlertRow> loadAuditAlerts();

    List<ActiveOrderRow> loadActiveOrders();

    TodayFinancials loadTodayFinancials();

    List<RevenueTrendRow> loadTodayRevenueTrend();

    String loadInventoryName(String alertId);

    record AlertRow(String id, String severity, Instant createdAt) {
    }

    record AuditAlertRow(String id, String action, String reason, Instant recordedAt) {
    }

    record ActiveOrderRow(String orderId, UUID serverUserId, int tableNumber, String status, Instant createdAt, int itemCount) {
    }

    record TodayFinancials(BigDecimal revenue, long transactions, BigDecimal refunds) {
        public TodayFinancials {
            revenue = revenue == null ? BigDecimal.ZERO : revenue;
            refunds = refunds == null ? BigDecimal.ZERO : refunds;
        }
    }

    record RevenueTrendRow(String hour, BigDecimal revenue) {
        public RevenueTrendRow {
            hour = hour == null ? "" : hour;
            revenue = revenue == null ? BigDecimal.ZERO : revenue;
        }
    }
}
