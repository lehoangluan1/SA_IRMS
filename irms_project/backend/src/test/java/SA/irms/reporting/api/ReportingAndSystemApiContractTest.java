package SA.irms.reporting.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import SA.irms.common.error.UnauthorizedException;
import SA.irms.common.security.CurrentUser;
import SA.irms.common.security.PermissionGuard;
import SA.irms.notification.api.NotificationController;
import SA.irms.notification.application.NotificationInboxService;
import SA.irms.reporting.application.DashboardService;
import SA.irms.reporting.application.ReportingQueryService;
import SA.irms.reporting.application.view.ReportingViews;
import SA.irms.common.api.HealthController;
import SA.irms.common.application.HealthStatusService;
import SA.irms.support.ApiContractTestSupport;
import SA.irms.support.TestFixtures;

@ExtendWith(MockitoExtension.class)
class ReportingAndSystemApiContractTest extends ApiContractTestSupport {

    @Mock
    private DashboardService dashboardService;
    @Mock
    private ReportingQueryService reportingQueryService;
    @Mock
    private PermissionGuard permissionGuard;
    @Mock
    private HealthStatusService healthStatusService;
    @Mock
    private NotificationInboxService notificationInboxService;
    @Mock
    private CurrentUser currentUser;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(permissionGuard.require(anyString())).thenReturn(TestFixtures.authenticatedUser("all"));
        lenient().when(currentUser.require()).thenReturn(TestFixtures.authenticatedUser("all"));
        mockMvc = mockMvcFor(
                new DashboardController(dashboardService, permissionGuard),
                new ReportingController(reportingQueryService, permissionGuard),
                new NotificationController(notificationInboxService, currentUser),
                new HealthController(healthStatusService, "irms-api-gateway")
        );
    }

    @Test
    void dashboardContractMatchesBackendKpisAlertsAndActiveOrders() throws Exception {
        when(dashboardService.load()).thenReturn(TestFixtures.dashboardView());

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.data.operations.activeTables").value(12))
                .andExpect(jsonPath("$.data.operations.revenueToday").value(4280.0))
                .andExpect(jsonPath("$.data.operations.revenueTrend[0].hour").value("10AM"))
                .andExpect(jsonPath("$.data.metrics[0].code").value("liveCovers"))
                .andExpect(jsonPath("$.data.kpis[0].trend").value("UP"))
                .andExpect(jsonPath("$.data.activeOrders[0].server").value("Alex Manager"));
    }

    @Test
    void reportingEndpointsCoverOperationsPeakHoursComboSalesAndExport() throws Exception {
        when(reportingQueryService.operationsReport()).thenReturn(TestFixtures.operationsReport());
        when(reportingQueryService.getSalesReport()).thenReturn(TestFixtures.salesReport());
        when(reportingQueryService.getPeakHourReport()).thenReturn(TestFixtures.peakHourReport());
        when(reportingQueryService.getBestSellingItemReport()).thenReturn(TestFixtures.bestSellingItemReport());
        when(reportingQueryService.getRevenueReport()).thenReturn(TestFixtures.revenueReport());
        when(reportingQueryService.getKitchenBottleneckReport()).thenReturn(TestFixtures.kitchenBottleneckReport());
        when(reportingQueryService.getStaffEfficiencyReport()).thenReturn(TestFixtures.staffEfficiencyReport());
        when(reportingQueryService.getInventoryUsageReport()).thenReturn(TestFixtures.inventoryUsageReport());
        when(reportingQueryService.getComboSalesReport()).thenReturn(TestFixtures.comboSalesReport());
        when(reportingQueryService.export("combo-sales", "csv"))
                .thenReturn(new ReportingViews.ExportedReport("combo-sales.csv", MediaType.TEXT_PLAIN, "combo".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/reports/operations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.openOrders").value(8))
                .andExpect(jsonPath("$.data.weeklyRevenue[0].day").value("Mon"))
                .andExpect(jsonPath("$.data.outboxEventId").value("outbox-1"));

        mockMvc.perform(get("/api/reports/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.rows[0].grossSales").value(4100.0));

        mockMvc.perform(get("/api/reports/peak-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.rows[0].hourOfDay").value(19))
                .andExpect(jsonPath("$.data.metrics[0].key").value("peakRevenue"));

        mockMvc.perform(get("/api/reports/best-selling-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[0].itemName").value("Grilled Ribeye"));

        mockMvc.perform(get("/api/reports/revenue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[0].paymentMethod").value("CARD"));

        mockMvc.perform(get("/api/reports/kitchen-bottlenecks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[0].station").value("grill"));

        mockMvc.perform(get("/api/reports/staff-efficiency"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[0].role").value("server"));

        mockMvc.perform(get("/api/reports/inventory-usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[0].ingredientName").value("Salmon"));

        mockMvc.perform(get("/api/reports/combo-sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.rows[0].comboName").value("Lunch Set"))
                .andExpect(jsonPath("$.data.metrics[0].key").value("comboRevenue"));

        mockMvc.perform(get("/api/reports/export")
                        .queryParam("type", "combo-sales")
                        .queryParam("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"combo-sales.csv\""))
                .andExpect(content().string("combo"));
    }

    @Test
    void healthEndpointReflectsDatabaseStateAndRemainsPublicShape() throws Exception {
        when(healthStatusService.isDatabaseUp()).thenReturn(true, false);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("irms-api-gateway"))
                .andExpect(jsonPath("$.data.database").value("UP"))
                .andExpect(jsonPath("$.data.status").value("UP"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.database").value("DOWN"));
    }

    @Test
    void notificationInboxAndReadEndpointsUseCurrentUserSession() throws Exception {
        when(notificationInboxService.loadInbox(any())).thenReturn(List.of(TestFixtures.notificationView()));
        when(notificationInboxService.markRead(any(), any())).thenReturn(TestFixtures.notificationView());

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(TestFixtures.MESSAGE_ID.toString()))
                .andExpect(jsonPath("$.data[0].status").value("UNREAD"));

        mockMvc.perform(post("/api/notifications/{messageId}/read", TestFixtures.MESSAGE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Kitchen delay"));
    }

    @Test
    void unauthorizedNotificationAccessUsesStandardErrorEnvelope() throws Exception {
        when(currentUser.require()).thenThrow(new UnauthorizedException("Authentication is required."));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required."))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }
}
