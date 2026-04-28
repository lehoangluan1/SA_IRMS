package SA.irms.reporting.application.port.out;

import SA.irms.reporting.application.view.ReportingProjectionRows;
import SA.irms.common.events.EventEnvelope;
import java.util.List;
import java.util.UUID;

public interface ReportingProjectionRepository extends ReportSnapshotRepository {
    void recordEventProjection(EventEnvelope envelope);

    void materializeSalesProjection(EventEnvelope envelope);

    void materializePeakHourProjection(EventEnvelope envelope);

    void materializeBestSellingItemProjection(EventEnvelope envelope);

    void materializeRevenueProjection(EventEnvelope envelope);

    void materializeKitchenBottleneckProjection(EventEnvelope envelope);

    void materializeStaffEfficiencyProjection(EventEnvelope envelope);

    void materializeInventoryUsageProjection(EventEnvelope envelope);

    void materializeComboSalesProjection(EventEnvelope envelope);

    boolean alreadyRefreshedForEvent(UUID outboxEventId);

    OperationsMetrics loadOperationsMetrics();

    List<ReportingProjectionRows.SalesReportRow> findSalesRows();

    List<ReportingProjectionRows.PeakHourReportRow> findPeakHourRows();

    List<ReportingProjectionRows.BestSellingItemReportRow> findBestSellingItemRows();

    List<ReportingProjectionRows.RevenueReportRow> findRevenueRows();

    List<ReportingProjectionRows.KitchenBottleneckReportRow> findKitchenBottleneckRows();

    List<ReportingProjectionRows.StaffEfficiencyReportRow> findStaffEfficiencyRows();

    List<ReportingProjectionRows.InventoryUsageReportRow> findInventoryUsageRows();

    List<ReportingProjectionRows.ComboSalesReportRow> findComboSalesRows();

    record OperationsMetrics(
            long activeTables,
            long openOrders,
            long readyToServe,
            long kitchenQueue,
            long averageKitchenWaitMinutes,
            long openLowStockAlerts
    ) {
    }
}
