package SA.irms.billing.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.billing.application.BillingService;
import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.security.PermissionGuard;
import SA.irms.common.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/billing")
public class BillingController {
    private final BillingService billingService;
    private final PermissionGuard permissionGuard;

    public BillingController(BillingService billingService, PermissionGuard permissionGuard) {
        this.billingService = billingService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/overview")
    public ApiEnvelope<SA.irms.billing.application.view.BillingViews.BillingOverview> overview(
            @RequestParam(value = "billId", required = false) UUID billId,
            @RequestParam(value = "sessionId", required = false) UUID sessionId,
            HttpServletRequest request
    ) {
        permissionGuard.require("billing.manage");
        return ApiEnvelope.of(billingService.load(billId, sessionId), RequestContext.getCorrelationId(request));
    }
}
