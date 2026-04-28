package SA.irms.billing.api;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/payments")
public class PaymentsController {
    private final BillingService billingService;
    private final PermissionGuard permissionGuard;

    public PaymentsController(BillingService billingService, PermissionGuard permissionGuard) {
        this.billingService = billingService;
        this.permissionGuard = permissionGuard;
    }

    @PostMapping("/{paymentId}/receipt")
    public ApiEnvelope<SA.irms.billing.application.view.BillingViews.ReceiptView> receipt(@PathVariable UUID paymentId, @Valid @RequestBody ReceiptBody requestBody, HttpServletRequest request) {
        permissionGuard.require("payments.process");
        return ApiEnvelope.of(billingService.issueReceipt(paymentId, requestBody.toRequest()), RequestContext.getCorrelationId(request));
    }

    @GetMapping(value = "/{paymentId}/receipt/document", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> receiptDocument(@PathVariable UUID paymentId) {
        permissionGuard.require("payments.process");
        byte[] document = billingService.buildReceiptDocument(paymentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"receipt-" + paymentId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(document);
    }

    @PostMapping("/{paymentId}/refunds")
    public ApiEnvelope<SA.irms.billing.application.view.BillingViews.RefundView> refund(@PathVariable UUID paymentId, @Valid @RequestBody RefundBody requestBody, HttpServletRequest request) {
        var actor = permissionGuard.require("billing.refund");
        return ApiEnvelope.of(billingService.refundPayment(paymentId, requestBody.toRequest(), actor, RequestContext.getCorrelationId(request), RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    public record ReceiptBody(@NotBlank String channel, String recipientAddress) {
        SA.irms.billing.application.command.BillingCommands.ReceiptRequest toRequest() {
            return new SA.irms.billing.application.command.BillingCommands.ReceiptRequest(channel, recipientAddress);
        }
    }

    public record RefundBody(BigDecimal amount, @NotBlank String reason) {
        SA.irms.billing.application.command.BillingCommands.RefundRequest toRequest() {
            return new SA.irms.billing.application.command.BillingCommands.RefundRequest(amount, reason);
        }
    }
}
