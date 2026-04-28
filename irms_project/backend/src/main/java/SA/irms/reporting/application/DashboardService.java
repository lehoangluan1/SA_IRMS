package SA.irms.reporting.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import SA.irms.common.error.NotFoundException;
import SA.irms.reporting.application.port.out.OperationsDashboardQueryRepository;
import SA.irms.reporting.application.port.out.ReportingProjectionRepository;
import SA.irms.reporting.application.support.OperationsSnapshotViewMapper;
import SA.irms.reporting.application.view.DashboardViews;
import SA.irms.reporting.application.view.OperationsReportView;
import SA.irms.reporting.application.view.dynamic.DynamicReportSnapshot;
import SA.irms.common.identity.SharedIdentityDirectoryPort;

@Service
public class DashboardService {
    private final ReportingProjectionRepository projectionRepository;
    private final OperationsDashboardQueryRepository dashboardRepository;
    private final SharedIdentityDirectoryPort identityDirectoryPort;
    private final OperationsSnapshotViewMapper operationsSnapshotViewMapper;
    private final Clock clock;

    public DashboardService(ReportingProjectionRepository projectionRepository,
                            OperationsDashboardQueryRepository dashboardRepository,
                            SharedIdentityDirectoryPort identityDirectoryPort,
                            OperationsSnapshotViewMapper operationsSnapshotViewMapper,
                            Clock clock) {
        this.projectionRepository = projectionRepository;
        this.dashboardRepository = dashboardRepository;
        this.identityDirectoryPort = identityDirectoryPort;
        this.operationsSnapshotViewMapper = operationsSnapshotViewMapper;
        this.clock = clock;
    }

    public DashboardViews.DashboardView load() {
        DynamicReportSnapshot snapshot = projectionRepository.findLatestSnapshot("operations")
                .orElseThrow(() -> new NotFoundException("No dashboard snapshot is available."));
        ReportingProjectionRepository.OperationsMetrics liveMetrics = projectionRepository.loadOperationsMetrics();
        OperationsReportView operations = operationsSnapshotViewMapper.compose(snapshot, liveMetrics);
        OperationsDashboardQueryRepository.TodayFinancials todayFinancials = dashboardRepository.loadTodayFinancials();
        List<OperationsReportView.RevenueTrendPoint> revenueTrend = dashboardRepository.loadTodayRevenueTrend().stream()
                .map(row -> new OperationsReportView.RevenueTrendPoint(row.hour(), row.revenue()))
                .toList();
        operations = new OperationsReportView(
                operations.generatedAt(),
                operations.periodStart(),
                operations.periodEnd(),
                operations.activeTables(),
                operations.totalTables(),
                operations.openOrders(),
                operations.readyToServe(),
                operations.kitchenQueue(),
                operations.averageKitchenWaitMinutes(),
                operations.averageServiceTimeMinutes(),
                operations.openLowStockAlerts(),
                todayFinancials.revenue(),
                todayFinancials.transactions(),
                operations.reservationCountToday(),
                todayFinancials.refunds(),
                revenueTrend,
                operations.weeklyRevenue(),
                operations.topDishes(),
                operations.peakHours(),
                operations.categoryRevenue(),
                operations.kitchenMetrics(),
                operations.outboxEventId()
        );

        List<DashboardViews.AlertView> alerts = dashboardRepository.loadOpenLowStockAlerts().stream()
                .map(row -> new DashboardViews.AlertView(
                        toUuid(row.id()),
                        "warning",
                        "Low stock: " + dashboardRepository.loadInventoryName(row.id()),
                        relativeTime(row.createdAt())
                ))
                .toList();
        List<DashboardViews.AlertView> auditAlerts = dashboardRepository.loadAuditAlerts().stream()
                .map(row -> new DashboardViews.AlertView(
                        toUuid(row.id()),
                        "danger",
                        formatAuditAlertMessage(row.action(), row.reason()),
                        relativeTime(row.recordedAt())
                ))
                .toList();
        alerts = java.util.stream.Stream.concat(alerts.stream(), auditAlerts.stream()).limit(4).toList();

        List<OperationsDashboardQueryRepository.ActiveOrderRow> rows = dashboardRepository.loadActiveOrders();
        Map<UUID, String> serverNames = identityDirectoryPort.findDisplayNames(rows.stream()
                .map(OperationsDashboardQueryRepository.ActiveOrderRow::serverUserId)
                .distinct()
                .toList());
        List<DashboardViews.ActiveOrderView> activeOrders = rows.stream()
                .map(row -> new DashboardViews.ActiveOrderView(
                        toUuid(row.orderId()),
                        row.tableNumber(),
                        serverNames.getOrDefault(row.serverUserId(), row.serverUserId().toString()),
                        row.itemCount(),
                        row.status(),
                        relativeDuration(row.createdAt())
                ))
                .toList();

        List<DashboardViews.DashboardMetric> metrics = operations.metrics().stream()
                .map(metric -> new DashboardViews.DashboardMetric(metric.key(), metric.label(), metric.displayValue(), metric.valueType()))
                .toList();

        List<DashboardViews.DashboardKpi> kpis = List.of(
                new DashboardViews.DashboardKpi("activeTables", "Active Tables", String.valueOf(operations.activeTables()), "tables", DashboardViews.TrendDirection.UNKNOWN),
                new DashboardViews.DashboardKpi("openOrders", "Open Orders", String.valueOf(operations.openOrders()), "orders", DashboardViews.TrendDirection.UNKNOWN),
                new DashboardViews.DashboardKpi("readyToServe", "Ready To Serve", String.valueOf(operations.readyToServe()), "orders", DashboardViews.TrendDirection.UNKNOWN),
                new DashboardViews.DashboardKpi("kitchenQueue", "Kitchen Queue", String.valueOf(operations.kitchenQueue()), "items", DashboardViews.TrendDirection.UNKNOWN),
                new DashboardViews.DashboardKpi("revenueToday", "Revenue Today", operations.revenueToday().stripTrailingZeros().toPlainString(), "USD", DashboardViews.TrendDirection.UNKNOWN),
                new DashboardViews.DashboardKpi("reservationCountToday", "Reservations Today", String.valueOf(operations.reservationCountToday()), "bookings", DashboardViews.TrendDirection.UNKNOWN)
        );

        return new DashboardViews.DashboardView(operations, metrics, kpis, alerts, activeOrders);
    }

    private String relativeDuration(Instant createdAt) {
        long minutes = Duration.between(createdAt, Instant.now(clock)).toMinutes();
        return minutes + " min";
    }

    private String relativeTime(Instant createdAt) {
        long minutes = Duration.between(createdAt, Instant.now(clock)).toMinutes();
        if (minutes < 60) {
            return minutes + " min ago";
        }
        return (minutes / 60) + " hr ago";
    }

    private String formatAuditAlertMessage(String action, String reason) {
        String baseMessage = switch (action) {
            case "billing.refund.issued" -> "Refund issued";
            case "inventory.manual_adjustment" -> "Inventory updated manually";
            case "billing.bill.reopened" -> "Bill reopened";
            default -> "Manager review recommended";
        };
        return reason == null || reason.isBlank() ? baseMessage : baseMessage + ": " + reason;
    }

    private UUID toUuid(String value) {
        return UUID.fromString(value);
    }
}
