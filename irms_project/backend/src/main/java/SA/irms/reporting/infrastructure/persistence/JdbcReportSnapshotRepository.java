package SA.irms.reporting.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import SA.irms.common.json.JsonSupport;
import SA.irms.reporting.application.port.out.ReportingProjectionRepository;
import SA.irms.reporting.application.view.ReportingProjectionRows;
import SA.irms.reporting.application.view.dynamic.DynamicReportSnapshot;
import SA.irms.reporting.application.view.dynamic.ReportSnapshotQuery;
import SA.irms.reporting.application.view.dynamic.ReportSnapshotSummaryView;
import SA.irms.common.events.EventEnvelope;

@Repository
public class JdbcReportSnapshotRepository implements ReportingProjectionRepository {
    private final JdbcReportSnapshotCrudRepository snapshotCrudRepository;
    private final JdbcProjectionRefreshLogRepository projectionRefreshLogRepository;
    private final JdbcSalesProjectionMaterializer salesProjectionMaterializer;
    private final JdbcPeakHourProjectionMaterializer peakHourProjectionMaterializer;
    private final JdbcBestSellingItemProjectionMaterializer bestSellingItemProjectionMaterializer;
    private final JdbcRevenueProjectionMaterializer revenueProjectionMaterializer;
    private final JdbcKitchenBottleneckProjectionMaterializer kitchenBottleneckProjectionMaterializer;
    private final JdbcStaffEfficiencyProjectionMaterializer staffEfficiencyProjectionMaterializer;
    private final JdbcInventoryUsageProjectionMaterializer inventoryUsageProjectionMaterializer;
    private final JdbcComboSalesProjectionMaterializer comboSalesProjectionMaterializer;
    private final JdbcReportProjectionQueryRepository projectionQueryRepository;

    public JdbcReportSnapshotRepository(JdbcClient jdbcClient, JsonSupport jsonSupport, ObjectMapper objectMapper) {
        ReportSnapshotPayloadMapper payloadMapper = new ReportSnapshotPayloadMapper(objectMapper);
        this.snapshotCrudRepository = new JdbcReportSnapshotCrudRepository(jdbcClient, payloadMapper);
        this.projectionRefreshLogRepository = new JdbcProjectionRefreshLogRepository(jdbcClient, payloadMapper);
        this.salesProjectionMaterializer = new JdbcSalesProjectionMaterializer(jdbcClient);
        this.peakHourProjectionMaterializer = new JdbcPeakHourProjectionMaterializer(jdbcClient);
        this.bestSellingItemProjectionMaterializer = new JdbcBestSellingItemProjectionMaterializer(jdbcClient);
        this.revenueProjectionMaterializer = new JdbcRevenueProjectionMaterializer(jdbcClient);
        this.kitchenBottleneckProjectionMaterializer = new JdbcKitchenBottleneckProjectionMaterializer(jdbcClient);
        this.staffEfficiencyProjectionMaterializer = new JdbcStaffEfficiencyProjectionMaterializer(jdbcClient);
        this.inventoryUsageProjectionMaterializer = new JdbcInventoryUsageProjectionMaterializer(jdbcClient);
        this.comboSalesProjectionMaterializer = new JdbcComboSalesProjectionMaterializer(jdbcClient);
        this.projectionQueryRepository = new JdbcReportProjectionQueryRepository(jdbcClient);
    }

    @Override
    public Optional<DynamicReportSnapshot> findSnapshot(UUID snapshotId) {
        return snapshotCrudRepository.findSnapshot(snapshotId);
    }

    @Override
    public Optional<DynamicReportSnapshot> findLatestSnapshot(String reportCode) {
        return snapshotCrudRepository.findLatestSnapshot(reportCode);
    }

    @Override
    public List<ReportSnapshotSummaryView> findSnapshots(ReportSnapshotQuery query) {
        return snapshotCrudRepository.findSnapshots(query);
    }

    @Override
    public void saveSnapshot(DynamicReportSnapshot snapshot) {
        snapshotCrudRepository.saveSnapshot(snapshot);
    }

    @Override
    public void recordEventProjection(EventEnvelope envelope) {
        projectionRefreshLogRepository.recordEventProjection(envelope);
    }

    @Override
    public void materializeSalesProjection(EventEnvelope envelope) {
        salesProjectionMaterializer.materialize(envelope);
    }

    @Override
    public void materializePeakHourProjection(EventEnvelope envelope) {
        peakHourProjectionMaterializer.materialize(envelope);
    }

    @Override
    public void materializeBestSellingItemProjection(EventEnvelope envelope) {
        bestSellingItemProjectionMaterializer.materialize(envelope);
    }

    @Override
    public void materializeRevenueProjection(EventEnvelope envelope) {
        revenueProjectionMaterializer.materialize(envelope);
    }

    @Override
    public void materializeKitchenBottleneckProjection(EventEnvelope envelope) {
        kitchenBottleneckProjectionMaterializer.materialize(envelope);
    }

    @Override
    public void materializeStaffEfficiencyProjection(EventEnvelope envelope) {
        staffEfficiencyProjectionMaterializer.materialize(envelope);
    }

    @Override
    public void materializeInventoryUsageProjection(EventEnvelope envelope) {
        inventoryUsageProjectionMaterializer.materialize(envelope);
    }

    @Override
    public void materializeComboSalesProjection(EventEnvelope envelope) {
        comboSalesProjectionMaterializer.materialize(envelope);
    }

    @Override
    public boolean alreadyRefreshedForEvent(UUID outboxEventId) {
        return projectionRefreshLogRepository.alreadyRefreshedForEvent(outboxEventId);
    }

    @Override
    public OperationsMetrics loadOperationsMetrics() {
        return projectionQueryRepository.loadOperationsMetrics();
    }

    @Override
    public List<ReportingProjectionRows.SalesReportRow> findSalesRows() {
        return projectionQueryRepository.findSalesRows();
    }

    @Override
    public List<ReportingProjectionRows.PeakHourReportRow> findPeakHourRows() {
        return projectionQueryRepository.findPeakHourRows();
    }

    @Override
    public List<ReportingProjectionRows.BestSellingItemReportRow> findBestSellingItemRows() {
        return projectionQueryRepository.findBestSellingItemRows();
    }

    @Override
    public List<ReportingProjectionRows.RevenueReportRow> findRevenueRows() {
        return projectionQueryRepository.findRevenueRows();
    }

    @Override
    public List<ReportingProjectionRows.KitchenBottleneckReportRow> findKitchenBottleneckRows() {
        return projectionQueryRepository.findKitchenBottleneckRows();
    }

    @Override
    public List<ReportingProjectionRows.StaffEfficiencyReportRow> findStaffEfficiencyRows() {
        return projectionQueryRepository.findStaffEfficiencyRows();
    }

    @Override
    public List<ReportingProjectionRows.InventoryUsageReportRow> findInventoryUsageRows() {
        return projectionQueryRepository.findInventoryUsageRows();
    }

    @Override
    public List<ReportingProjectionRows.ComboSalesReportRow> findComboSalesRows() {
        return projectionQueryRepository.findComboSalesRows();
    }
}
