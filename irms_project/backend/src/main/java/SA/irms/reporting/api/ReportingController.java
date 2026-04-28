package SA.irms.reporting.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.security.PermissionGuard;
import SA.irms.common.web.RequestContext;
import SA.irms.reporting.application.ReportingQueryService;
import SA.irms.reporting.application.view.OperationsReportView;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/reports")
public class ReportingController {
    private final ReportingQueryService reportingQueryService;
    private final PermissionGuard permissionGuard;

    public ReportingController(ReportingQueryService reportingQueryService, PermissionGuard permissionGuard) {
        this.reportingQueryService = reportingQueryService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/operations")
    public ApiEnvelope<OperationsReportView> operations(HttpServletRequest request) {
        permissionGuard.require("reports.view");
        return ApiEnvelope.of(reportingQueryService.operationsReport(), RequestContext.getCorrelationId(request));
    }

    @GetMapping("/sales")
    public ApiEnvelope<SA.irms.reporting.application.view.TypedReportViews.SalesReportView> sales(HttpServletRequest request) {
        permissionGuard.require("reports.view");
        return ApiEnvelope.of(reportingQueryService.getSalesReport(), RequestContext.getCorrelationId(request));
    }

    @GetMapping("/peak-hours")
    public ApiEnvelope<SA.irms.reporting.application.view.TypedReportViews.PeakHourReportView> peakHours(HttpServletRequest request) {
        permissionGuard.require("reports.view");
        return ApiEnvelope.of(reportingQueryService.getPeakHourReport(), RequestContext.getCorrelationId(request));
    }

    @GetMapping("/best-selling-items")
    public ApiEnvelope<SA.irms.reporting.application.view.TypedReportViews.BestSellingItemReportView> bestSellingItems(HttpServletRequest request) {
        permissionGuard.require("reports.view");
        return ApiEnvelope.of(reportingQueryService.getBestSellingItemReport(), RequestContext.getCorrelationId(request));
    }

    @GetMapping("/revenue")
    public ApiEnvelope<SA.irms.reporting.application.view.TypedReportViews.RevenueReportView> revenue(HttpServletRequest request) {
        permissionGuard.require("reports.view");
        return ApiEnvelope.of(reportingQueryService.getRevenueReport(), RequestContext.getCorrelationId(request));
    }

    @GetMapping("/kitchen-bottlenecks")
    public ApiEnvelope<SA.irms.reporting.application.view.TypedReportViews.KitchenBottleneckReportView> kitchenBottlenecks(HttpServletRequest request) {
        permissionGuard.require("reports.view");
        return ApiEnvelope.of(reportingQueryService.getKitchenBottleneckReport(), RequestContext.getCorrelationId(request));
    }

    @GetMapping("/staff-efficiency")
    public ApiEnvelope<SA.irms.reporting.application.view.TypedReportViews.StaffEfficiencyReportView> staffEfficiency(HttpServletRequest request) {
        permissionGuard.require("reports.view");
        return ApiEnvelope.of(reportingQueryService.getStaffEfficiencyReport(), RequestContext.getCorrelationId(request));
    }

    @GetMapping("/inventory-usage")
    public ApiEnvelope<SA.irms.reporting.application.view.TypedReportViews.InventoryUsageReportView> inventoryUsage(HttpServletRequest request) {
        permissionGuard.require("reports.view");
        return ApiEnvelope.of(reportingQueryService.getInventoryUsageReport(), RequestContext.getCorrelationId(request));
    }

    @GetMapping("/combo-sales")
    public ApiEnvelope<SA.irms.reporting.application.view.TypedReportViews.ComboSalesReportView> comboSales(HttpServletRequest request) {
        permissionGuard.require("reports.view");
        return ApiEnvelope.of(reportingQueryService.getComboSalesReport(), RequestContext.getCorrelationId(request));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(value = "type", defaultValue = "operations") String type,
            @RequestParam(value = "format", defaultValue = "csv") String format
    ) {
        permissionGuard.require("reports.view");
        SA.irms.reporting.application.view.ReportingViews.ExportedReport report = reportingQueryService.export(type, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.filename() + "\"")
                .contentType(report.mediaType())
                .body(report.content());
    }
}
