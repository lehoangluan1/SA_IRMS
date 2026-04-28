package SA.irms.reporting.application.support;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import SA.irms.reporting.application.port.out.ReportingProjectionRepository;
import SA.irms.reporting.application.view.OperationsReportView;
import SA.irms.reporting.application.view.dynamic.DynamicReportCell;
import SA.irms.reporting.application.view.dynamic.DynamicReportSnapshot;
import SA.irms.reporting.application.view.dynamic.ReportSnapshotPayload;

@Component
public class OperationsSnapshotViewMapper {
    private final ObjectMapper objectMapper;

    public OperationsSnapshotViewMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OperationsReportView compose(
            DynamicReportSnapshot snapshot,
            ReportingProjectionRepository.OperationsMetrics liveMetrics
    ) {
        String outboxEventId = snapshot == null || snapshot.payload() == null ? null : snapshot.payload().sourceEventId();
        Instant generatedAt = snapshot == null ? Instant.now() : snapshot.generatedAt();
        return compose(snapshot, liveMetrics, generatedAt, outboxEventId);
    }

    public OperationsReportView compose(
            DynamicReportSnapshot snapshot,
            ReportingProjectionRepository.OperationsMetrics liveMetrics,
            Instant generatedAt,
            String outboxEventId
    ) {
        Instant safeGeneratedAt = generatedAt == null ? Instant.now() : generatedAt;
        Instant periodStart = snapshot != null && snapshot.periodStart() != null
                ? snapshot.periodStart()
                : safeGeneratedAt.minus(Duration.ofHours(24));
        Instant periodEnd = snapshot != null && snapshot.periodEnd() != null
                ? snapshot.periodEnd()
                : safeGeneratedAt;
        ReportSnapshotPayload payload = snapshot == null || snapshot.payload() == null
                ? ReportSnapshotPayload.empty("operations", "Operations Snapshot")
                : snapshot.payload();

        return new OperationsReportView(
                safeGeneratedAt,
                periodStart,
                periodEnd,
                liveMetrics.activeTables(),
                longMetric(payload, Math.max(liveMetrics.activeTables(), 0L), "totalTables"),
                liveMetrics.openOrders(),
                liveMetrics.readyToServe(),
                liveMetrics.kitchenQueue(),
                liveMetrics.averageKitchenWaitMinutes(),
                longMetric(payload, liveMetrics.averageKitchenWaitMinutes(), "averageServiceTimeMinutes", "avgServiceTimeMinutes"),
                liveMetrics.openLowStockAlerts(),
                decimalMetric(payload, BigDecimal.ZERO, "revenueToday"),
                longMetric(payload, 0L, "transactionsToday"),
                longMetric(payload, 0L, "reservationCountToday"),
                decimalMetric(payload, BigDecimal.ZERO, "refundsToday"),
                listMetric(payload, "revenueTrend", OperationsReportView.RevenueTrendPoint.class),
                listMetric(payload, "weeklyRevenue", OperationsReportView.WeeklyRevenuePoint.class),
                listMetric(payload, "topDishes", OperationsReportView.TopDishPoint.class),
                listMetric(payload, "peakHours", OperationsReportView.PeakHourPoint.class),
                listMetric(payload, "categoryRevenue", OperationsReportView.CategoryRevenuePoint.class),
                listMetric(payload, "kitchenMetrics", OperationsReportView.KitchenMetricPoint.class),
                outboxEventId
        );
    }

    private long longMetric(ReportSnapshotPayload payload, long fallback, String... keys) {
        Object raw = rawMetric(payload, keys);
        if (raw == null) {
            return fallback;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        try {
            return new BigDecimal(String.valueOf(raw)).longValue();
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private BigDecimal decimalMetric(ReportSnapshotPayload payload, BigDecimal fallback, String... keys) {
        Object raw = rawMetric(payload, keys);
        if (raw == null) {
            return fallback;
        }
        if (raw instanceof BigDecimal decimal) {
            return decimal;
        }
        if (raw instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(raw));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private <T> List<T> listMetric(ReportSnapshotPayload payload, String key, Class<T> itemType) {
        Object raw = rawMetric(payload, key);
        if (raw == null) {
            return List.of();
        }
        JavaType collectionType = objectMapper.getTypeFactory().constructCollectionType(List.class, itemType);
        try {
            return List.copyOf(objectMapper.convertValue(raw, collectionType));
        } catch (IllegalArgumentException exception) {
            if (raw instanceof String text && looksLikeJson(text)) {
                try {
                    return List.copyOf(objectMapper.readerFor(collectionType).readValue(text));
                } catch (IOException ignored) {
                    return List.of();
                }
            }
            return List.of();
        }
    }

    private Object rawMetric(ReportSnapshotPayload payload, String... keys) {
        if (payload == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            DynamicReportCell cell = payload.metricValue(key).orElse(null);
            if (cell == null) {
                continue;
            }
            if (cell.typedValue() != null) {
                return cell.typedValue();
            }
            if (!cell.displayValue().isBlank()) {
                return cell.displayValue();
            }
        }
        return null;
    }

    private boolean looksLikeJson(String value) {
        String text = value == null ? "" : value.trim();
        return text.startsWith("[") || text.startsWith("{");
    }
}
