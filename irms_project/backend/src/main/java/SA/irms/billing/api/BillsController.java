package SA.irms.billing.api;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

import org.springframework.web.bind.annotation.PatchMapping;
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
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/bills")
public class BillsController {
    private final BillingService billingService;
    private final PermissionGuard permissionGuard;

    public BillsController(BillingService billingService, PermissionGuard permissionGuard) {
        this.billingService = billingService;
        this.permissionGuard = permissionGuard;
    }

    @PostMapping
    public ApiEnvelope<SA.irms.billing.application.view.BillingViews.BillView> createBill(@Valid @RequestBody CreateBillBody requestBody, HttpServletRequest request) {
        permissionGuard.require("billing.manage");
        return ApiEnvelope.of(billingService.createBill(requestBody.tableSessionId()), RequestContext.getCorrelationId(request));
    }

    @PatchMapping("/{billId}")
    public ApiEnvelope<SA.irms.billing.application.view.BillingViews.BillView> updateBill(@PathVariable UUID billId, @Valid @RequestBody UpdateBillBody requestBody, HttpServletRequest request) {
        var actor = permissionGuard.require("billing.manage");
        return ApiEnvelope.of(billingService.updateBill(billId, requestBody.tipAmount(), requestBody.discountAmount(), actor, RequestContext.getCorrelationId(request), RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/{billId}/promotions")
    public ApiEnvelope<SA.irms.billing.application.view.BillingViews.BillView> applyPromotion(@PathVariable UUID billId, @Valid @RequestBody PromotionBody requestBody, HttpServletRequest request) {
        permissionGuard.require("billing.manage");
        return ApiEnvelope.of(billingService.applyPromotion(billId, requestBody.code()), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/{billId}/splits")
    public ApiEnvelope<SA.irms.billing.application.view.BillingViews.BillView> splitBill(@PathVariable UUID billId, @Valid @RequestBody SplitBody requestBody, HttpServletRequest request) {
        permissionGuard.require("billing.manage");
        return ApiEnvelope.of(billingService.splitBill(billId, requestBody.toRequest()), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/{billId}/payments")
    public ApiEnvelope<SA.irms.billing.application.view.BillingViews.PaymentView> pay(@PathVariable UUID billId, @Valid @RequestBody PaymentBody requestBody, HttpServletRequest request) {
        var actor = permissionGuard.require("payments.process");
        return ApiEnvelope.of(billingService.processPayment(billId, requestBody.toRequest(), actor), RequestContext.getCorrelationId(request));
    }

    public record CreateBillBody(@NotNull UUID tableSessionId) {
    }

    public record UpdateBillBody(@NotNull BigDecimal tipAmount, @NotNull BigDecimal discountAmount) {
    }

    public record PromotionBody(@NotBlank String code) {
    }

    public record SplitBody(String method, Integer splitCount, java.util.List<BigDecimal> amounts, java.util.List<BigDecimal> tipAmounts) {
        SA.irms.billing.application.command.BillingCommands.SplitRequest toRequest() {
            return new SA.irms.billing.application.command.BillingCommands.SplitRequest(method, splitCount, amounts, tipAmounts);
        }
    }

    public record PaymentBody(UUID splitId, @NotBlank String method, BigDecimal amount, BigDecimal amountReceived) {
        SA.irms.billing.application.command.BillingCommands.PaymentRequest toRequest() {
            return new SA.irms.billing.application.command.BillingCommands.PaymentRequest(splitId, method, amount, amountReceived);
        }
    }
}
