package SA.irms.billing.application;

import SA.irms.billing.application.port.out.BillAdjustmentRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.common.audit.AuditRecorder;
import SA.irms.billing.application.port.out.PromotionGatewayPort;
import SA.irms.common.context.RequestMetadata;

@Service
public class BillingAdjustmentService {
    private final BillAdjustmentRepository repository;
    private final AuditRecorder auditService;
    private final BillingReadService billingReadService;
    private final PromotionGatewayPort promotionGatewayPort;

    public BillingAdjustmentService(BillAdjustmentRepository repository,
                                    AuditRecorder auditService,
                                    BillingReadService billingReadService,
                                    PromotionGatewayPort promotionGatewayPort) {
        this.repository = repository;
        this.auditService = auditService;
        this.billingReadService = billingReadService;
        this.promotionGatewayPort = promotionGatewayPort;
    }

    @Transactional
    public SA.irms.billing.application.view.BillingViews.BillView updateBill(UUID billId,
            BigDecimal tipAmount,
            BigDecimal discountAmount,
            AuthenticatedUser actor,
            String correlationId,
            RequestMetadata httpServletRequest) {
        SA.irms.billing.application.view.BillingViews.BillView existing = billingReadService.findBill(billId);
        repository.updateBillTotals(billId, tipAmount, discountAmount);
        repository.syncSingleSplit(billId, tipAmount);
        if (discountAmount.compareTo(existing.discount()) != 0) {
            auditService.record(
                    actor.userId(),
                    "billing.price_override",
                    "Bill",
                    billId.toString(),
                    correlationId,
                    "Manual discount adjustment.",
                    false,
                    httpServletRequest.remoteIp(),
                    Map.of("discount", existing.discount()),
                    Map.of("discount", discountAmount)
            );
        }
        return billingReadService.findBill(billId);
    }

    @Transactional
    public SA.irms.billing.application.view.BillingViews.BillView applyPromotion(UUID billId, String code) {
        promotionGatewayPort.applyPromotion(billId, code);
        return billingReadService.findBill(billId);
    }
}
