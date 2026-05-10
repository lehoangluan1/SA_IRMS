package SA.irms.reporting.application.view;

import java.util.List;
import java.util.UUID;

public final class DashboardViews {
    private DashboardViews() {}

    public record DashboardView(
            OperationsReportView operations,
            List<DashboardMetric> metrics,
            List<DashboardKpi> kpis,
            List<AlertView> alerts,
            List<ActiveOrderView> activeOrders
    ) {
        public DashboardView {
            metrics = List.copyOf(metrics == null ? List.of() : metrics);
            kpis = List.copyOf(kpis == null ? List.of() : kpis);
            alerts = List.copyOf(alerts == null ? List.of() : alerts);
            activeOrders = List.copyOf(activeOrders == null ? List.of() : activeOrders);
        }
    }

    public record DashboardMetric(
            String code,
            String label,
            String displayValue,
            ReportValueType valueType
    ) {
    }

    public record DashboardKpi(
            String code,
            String label,
            String displayValue,
            String unit,
            TrendDirection trend
    ) {
    }

    public enum TrendDirection {
        UP,
        DOWN,
        FLAT,
        UNKNOWN
    }

    public record AlertView(UUID id, String type, String message, String time) {}
    public record ActiveOrderView(UUID id, int table, String server, int items, String status, String time) {}
}
