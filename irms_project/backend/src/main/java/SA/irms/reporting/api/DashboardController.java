package SA.irms.reporting.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.security.PermissionGuard;
import SA.irms.common.web.RequestContext;
import SA.irms.reporting.application.DashboardService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;
    private final PermissionGuard permissionGuard;

    public DashboardController(DashboardService dashboardService, PermissionGuard permissionGuard) {
        this.dashboardService = dashboardService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiEnvelope<SA.irms.reporting.application.view.DashboardViews.DashboardView> dashboard(HttpServletRequest request) {
        permissionGuard.require("dashboard.view");
        return ApiEnvelope.of(dashboardService.load(), RequestContext.getCorrelationId(request));
    }
}
