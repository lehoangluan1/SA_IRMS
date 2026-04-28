package SA.irms.reporting.application;

import SA.irms.common.error.ConflictException;
import SA.irms.common.error.NotFoundException;
import SA.irms.reporting.application.port.out.ReportingProjectionRepository;
import SA.irms.reporting.application.support.OperationsSnapshotViewMapper;
import SA.irms.reporting.application.support.ReportTableViewFactory;
import SA.irms.reporting.application.view.OperationsReportView;
import SA.irms.reporting.application.view.ReportExportView;
import SA.irms.reporting.application.view.ReportMetricView;
import SA.irms.reporting.application.view.ReportValueType;
import SA.irms.reporting.application.view.ReportingProjectionRows;
import SA.irms.reporting.application.view.TypedReportViews;
import SA.irms.reporting.application.view.dynamic.DynamicReportSnapshot;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ReportingQueryService {
    private final ReportingProjectionRepository repository;
    private final OperationsSnapshotViewMapper operationsSnapshotViewMapper;
    private final ReportTableViewFactory reportTableViewFactory;
    private final Clock clock;
    private final java.util.Map<String, ReportExporter> exportersByFormat;

    public ReportingQueryService(
            ReportingProjectionRepository repository,
            OperationsSnapshotViewMapper operationsSnapshotViewMapper,
            ReportTableViewFactory reportTableViewFactory,
            Clock clock,
            List<ReportExporter> exporters
    ) {
        this.repository = repository;
        this.operationsSnapshotViewMapper = operationsSnapshotViewMapper;
        this.reportTableViewFactory = reportTableViewFactory;
        this.clock = clock;
        this.exportersByFormat = exporters.stream()
                .collect(Collectors.toUnmodifiableMap(
                        exporter -> exporter.fileExtension().toLowerCase(),
                        Function.identity(),
                        (first, second) -> first
                ));
    }

    public OperationsReportView operationsReport() {
        DynamicReportSnapshot snapshot = repository.findLatestSnapshot("operations")
                .orElseThrow(() -> new NotFoundException("No operations report snapshot is available."));
        ReportingProjectionRepository.OperationsMetrics liveMetrics = repository.loadOperationsMetrics();
        return operationsSnapshotViewMapper.compose(snapshot, liveMetrics);
    }

    public TypedReportViews.SalesReportView getSalesReport() {
        List<ReportingProjectionRows.SalesReportRow> rows = repository.findSalesRows();
        return new TypedReportViews.SalesReportView(
                now(),
                periodStart(rows.stream().map(ReportingProjectionRows.SalesReportRow::businessDate).toList()),
                now(),
                rows,
                List.of(
                        metric("salesRows", "Sales Rows", rows.size()),
                        moneyMetric("netSalesTotal", "Net Sales Total",
                                rows.stream().map(ReportingProjectionRows.SalesReportRow::netSales).reduce(BigDecimal.ZERO, BigDecimal::add))
                )
        );
    }

    public TypedReportViews.PeakHourReportView getPeakHourReport() {
        List<ReportingProjectionRows.PeakHourReportRow> rows = repository.findPeakHourRows();
        return new TypedReportViews.PeakHourReportView(
                now(),
                periodStart(rows.stream().map(ReportingProjectionRows.PeakHourReportRow::businessDate).toList()),
                now(),
                rows,
                List.of(metric("peakHourRows", "Peak Hour Rows", rows.size()))
        );
    }

    public TypedReportViews.BestSellingItemReportView getBestSellingItemReport() {
        List<ReportingProjectionRows.BestSellingItemReportRow> rows = repository.findBestSellingItemRows();
        return new TypedReportViews.BestSellingItemReportView(
                now(),
                periodStart(rows.stream().map(ReportingProjectionRows.BestSellingItemReportRow::businessDate).toList()),
                now(),
                rows,
                List.of(metric("bestSellingItemRows", "Best Selling Item Rows", rows.size()))
        );
    }

    public TypedReportViews.RevenueReportView getRevenueReport() {
        List<ReportingProjectionRows.RevenueReportRow> rows = repository.findRevenueRows();
        return new TypedReportViews.RevenueReportView(
                now(),
                periodStart(rows.stream().map(ReportingProjectionRows.RevenueReportRow::businessDate).toList()),
                now(),
                rows,
                List.of(
                        moneyMetric("netRevenueTotal", "Net Revenue Total",
                                rows.stream().map(ReportingProjectionRows.RevenueReportRow::netRevenue).reduce(BigDecimal.ZERO, BigDecimal::add))
                )
        );
    }

    public TypedReportViews.KitchenBottleneckReportView getKitchenBottleneckReport() {
        List<ReportingProjectionRows.KitchenBottleneckReportRow> rows = repository.findKitchenBottleneckRows();
        return new TypedReportViews.KitchenBottleneckReportView(
                now(),
                periodStart(rows.stream().map(ReportingProjectionRows.KitchenBottleneckReportRow::businessDate).toList()),
                now(),
                rows,
                List.of(metric("kitchenBottleneckRows", "Kitchen Bottleneck Rows", rows.size()))
        );
    }

    public TypedReportViews.StaffEfficiencyReportView getStaffEfficiencyReport() {
        List<ReportingProjectionRows.StaffEfficiencyReportRow> rows = repository.findStaffEfficiencyRows();
        return new TypedReportViews.StaffEfficiencyReportView(
                now(),
                periodStart(rows.stream().map(ReportingProjectionRows.StaffEfficiencyReportRow::businessDate).toList()),
                now(),
                rows,
                List.of(metric("staffEfficiencyRows", "Staff Efficiency Rows", rows.size()))
        );
    }

    public TypedReportViews.InventoryUsageReportView getInventoryUsageReport() {
        List<ReportingProjectionRows.InventoryUsageReportRow> rows = repository.findInventoryUsageRows();
        return new TypedReportViews.InventoryUsageReportView(
                now(),
                periodStart(rows.stream().map(ReportingProjectionRows.InventoryUsageReportRow::businessDate).toList()),
                now(),
                rows,
                List.of(metric("inventoryUsageRows", "Inventory Usage Rows", rows.size()))
        );
    }

    public TypedReportViews.ComboSalesReportView getComboSalesReport() {
        List<ReportingProjectionRows.ComboSalesReportRow> rows = repository.findComboSalesRows();
        return new TypedReportViews.ComboSalesReportView(
                now(),
                periodStart(rows.stream().map(ReportingProjectionRows.ComboSalesReportRow::businessDate).toList()),
                now(),
                rows,
                List.of(
                        metric("comboSalesRows", "Combo Sales Rows", rows.size()),
                        moneyMetric("comboRevenueTotal", "Combo Revenue Total",
                                rows.stream().map(ReportingProjectionRows.ComboSalesReportRow::revenueTotal).reduce(BigDecimal.ZERO, BigDecimal::add))
                )
        );
    }

    public SA.irms.reporting.application.view.ReportingViews.ExportedReport export(String type, String format) {
        String normalizedType = normalizeType(type);
        ReportExportView report = exportView(normalizedType);
        ReportExporter exporter = exportersByFormat.get(normalize(format));
        if (exporter == null) {
            throw new ConflictException("Unsupported report export format.");
        }
        return new SA.irms.reporting.application.view.ReportingViews.ExportedReport(
                normalizedType + "-report." + exporter.fileExtension(),
                exporter.mediaType(),
                exporter.export(report)
        );
    }

    private ReportExportView exportView(String normalizedType) {
        return switch (normalizedType) {
            case "operations" -> reportTableViewFactory.create(operationsReport());
            case "sales" -> reportTableViewFactory.create(getSalesReport());
            case "peak-hours", "peak_hours" -> reportTableViewFactory.create(getPeakHourReport());
            case "best-selling-items", "best_selling_items" -> reportTableViewFactory.create(getBestSellingItemReport());
            case "revenue" -> reportTableViewFactory.create(getRevenueReport());
            case "kitchen-bottlenecks", "kitchen_bottlenecks" -> reportTableViewFactory.create(getKitchenBottleneckReport());
            case "staff-efficiency", "staff_efficiency" -> reportTableViewFactory.create(getStaffEfficiencyReport());
            case "inventory-usage", "inventory_usage" -> reportTableViewFactory.create(getInventoryUsageReport());
            case "combo-sales", "combo_sales" -> reportTableViewFactory.create(getComboSalesReport());
            default -> throw new NotFoundException("No " + normalizedType + " report is available.");
        };
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private Instant periodStart(List<LocalDate> dates) {
        return dates.stream()
                .min(LocalDate::compareTo)
                .map(date -> date.atStartOfDay().toInstant(ZoneOffset.UTC))
                .orElseGet(this::now);
    }

    private String normalize(String format) {
        return format == null || format.isBlank() ? "csv" : format.trim().toLowerCase();
    }

    private String normalizeType(String type) {
        return type == null ? "operations" : type.trim().toLowerCase();
    }

    private ReportMetricView metric(String key, String label, long value) {
        return new ReportMetricView(key, label, ReportValueType.INTEGER, String.valueOf(value));
    }

    private ReportMetricView moneyMetric(String key, String label, BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return new ReportMetricView(key, label, ReportValueType.MONEY, safe.stripTrailingZeros().toPlainString());
    }
}
