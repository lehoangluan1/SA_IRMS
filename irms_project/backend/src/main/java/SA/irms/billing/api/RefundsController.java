package SA.irms.billing.api;

import java.util.UUID;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.billing.application.BillingService;
import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.security.PermissionGuard;
import SA.irms.common.web.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/refunds")
public class RefundsController {
    private final BillingService billingService;
    private final PermissionGuard permissionGuard;

    public RefundsController(BillingService billingService, PermissionGuard permissionGuard) {
        this.billingService = billingService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/pending")
    public ApiEnvelope<java.util.List<SA.irms.billing.application.view.BillingViews.RefundView>> pendingRefunds(HttpServletRequest request) {
        permissionGuard.require("billing.refund");
        return ApiEnvelope.of(billingService.pendingRefunds(), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/{refundId}/approval")
    public ApiEnvelope<SA.irms.billing.application.view.BillingViews.RefundView> approveRefund(@PathVariable UUID refundId, @Valid @RequestBody RefundApprovalBody requestBody, HttpServletRequest request) {
        var actor = permissionGuard.require("billing.refund");
        return ApiEnvelope.of(billingService.reviewRefund(refundId, requestBody.toRequest(), actor, RequestContext.getCorrelationId(request), RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    public record RefundApprovalBody(@NotBlank String action, String reason) {
        SA.irms.billing.application.command.BillingCommands.RefundApprovalRequest toRequest() {
            return new SA.irms.billing.application.command.BillingCommands.RefundApprovalRequest(action, reason);
        }
    }
}
