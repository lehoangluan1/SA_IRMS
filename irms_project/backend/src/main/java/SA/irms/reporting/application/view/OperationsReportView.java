package SA.irms.reporting.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OperationsReportView(
        Instant generatedAt,
        Instant periodStart,
        Instant periodEnd,
        long activeTables,
        long totalTables,
        long openOrders,
        long readyToServe,
        long kitchenQueue,
        long averageKitchenWaitMinutes,
        long averageServiceTimeMinutes,
        long openLowStockAlerts,
        BigDecimal revenueToday,
        long transactionsToday,
        long reservationCountToday,
        BigDecimal refundsToday,
        List<RevenueTrendPoint> revenueTrend,
        List<WeeklyRevenuePoint> weeklyRevenue,
        List<TopDishPoint> topDishes,
        List<PeakHourPoint> peakHours,
        List<CategoryRevenuePoint> categoryRevenue,
        List<KitchenMetricPoint> kitchenMetrics,
        String outboxEventId
) {
    public OperationsReportView {
        revenueToday = revenueToday == null ? BigDecimal.ZERO : revenueToday;
        refundsToday = refundsToday == null ? BigDecimal.ZERO : refundsToday;
        revenueTrend = List.copyOf(revenueTrend == null ? List.of() : revenueTrend);
        weeklyRevenue = List.copyOf(weeklyRevenue == null ? List.of() : weeklyRevenue);
        topDishes = List.copyOf(topDishes == null ? List.of() : topDishes);
        peakHours = List.copyOf(peakHours == null ? List.of() : peakHours);
        categoryRevenue = List.copyOf(categoryRevenue == null ? List.of() : categoryRevenue);
        kitchenMetrics = List.copyOf(kitchenMetrics == null ? List.of() : kitchenMetrics);
    }

    public List<ReportMetricView> metrics() {
        return List.of(
                metric("activeTables", "Active Tables", ReportValueType.INTEGER, activeTables),
                metric("totalTables", "Total Tables", ReportValueType.INTEGER, totalTables),
                metric("openOrders", "Open Orders", ReportValueType.INTEGER, openOrders),
                metric("readyToServe", "Ready To Serve", ReportValueType.INTEGER, readyToServe),
                metric("kitchenQueue", "Kitchen Queue", ReportValueType.INTEGER, kitchenQueue),
                metric("averageKitchenWaitMinutes", "Average Kitchen Wait (Minutes)", ReportValueType.DURATION_MINUTES, averageKitchenWaitMinutes),
                metric("averageServiceTimeMinutes", "Average Service Time (Minutes)", ReportValueType.DURATION_MINUTES, averageServiceTimeMinutes),
                metric("openLowStockAlerts", "Open Low Stock Alerts", ReportValueType.INTEGER, openLowStockAlerts),
                moneyMetric("revenueToday", "Revenue Today", revenueToday),
                metric("transactionsToday", "Transactions Today", ReportValueType.INTEGER, transactionsToday),
                metric("reservationCountToday", "Reservations Today", ReportValueType.INTEGER, reservationCountToday),
                moneyMetric("refundsToday", "Refunds Today", refundsToday)
        );
    }

    private ReportMetricView metric(String key, String label, ReportValueType type, long value) {
        return new ReportMetricView(key, label, type, String.valueOf(value));
    }

    private ReportMetricView moneyMetric(String key, String label, BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return new ReportMetricView(key, label, ReportValueType.MONEY, safe.stripTrailingZeros().toPlainString());
    }

    public record RevenueTrendPoint(String hour, BigDecimal revenue) {
        public RevenueTrendPoint {
            hour = hour == null ? "" : hour;
            revenue = revenue == null ? BigDecimal.ZERO : revenue;
        }
    }

    public record WeeklyRevenuePoint(String day, BigDecimal revenue) {
        public WeeklyRevenuePoint {
            day = day == null ? "" : day;
            revenue = revenue == null ? BigDecimal.ZERO : revenue;
        }
    }

    public record TopDishPoint(String name, long orders, BigDecimal revenue) {
        public TopDishPoint {
            name = name == null ? "" : name;
            revenue = revenue == null ? BigDecimal.ZERO : revenue;
        }
    }

    public record PeakHourPoint(String hour, long orders) {
        public PeakHourPoint {
            hour = hour == null ? "" : hour;
        }
    }

    public record CategoryRevenuePoint(String name, BigDecimal value) {
        public CategoryRevenuePoint {
            name = name == null ? "" : name;
            value = value == null ? BigDecimal.ZERO : value;
        }
    }

    public record KitchenMetricPoint(String station, String avgTime, long tickets, long delayed) {
        public KitchenMetricPoint {
            station = station == null ? "" : station;
            avgTime = avgTime == null ? "" : avgTime;
        }
    }
}
