package SA.irms.ordering.application;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.error.NotFoundException;
import SA.irms.ordering.application.port.out.OrderRepository;
import SA.irms.ordering.application.events.PromotionUpdatedEvent;
import SA.irms.common.outbox.DomainEventPublisher;

@Service
class MenuPromotionService {
    private final OrderRepository repository;
    private final PromotionDiscountParser discountParser;
    private final DomainEventPublisher outboxPublisher;

    MenuPromotionService(OrderRepository repository, PromotionDiscountParser discountParser, DomainEventPublisher outboxPublisher) {
        this.repository = repository;
        this.discountParser = discountParser;
        this.outboxPublisher = outboxPublisher;
    }

    @Transactional
    SA.irms.ordering.application.view.MenuViews.PromotionView createPromotion(SA.irms.ordering.application.command.MenuCommands.PromotionUpsert request) {
        PromotionDiscountParser.ParsedDiscount parsedDiscount = discountParser.parseDiscount(request.discount());
        UUID promotionId = UUID.randomUUID();
        repository.createPromotion(
                promotionId,
                request.code(),
                request.code() + " Promotion",
                parsedDiscount.type(),
                parsedDiscount.value(),
                discountParser.parseDate(request.validUntil()) == null ? null : java.sql.Timestamp.from(discountParser.parseDate(request.validUntil()))
        );
        publishPromotionUpdated(promotionId, request, "created");
        return new SA.irms.ordering.application.view.MenuViews.PromotionView(promotionId, request.code(), request.discount(), request.validUntil(), true);
    }

    @Transactional
    SA.irms.ordering.application.view.MenuViews.PromotionView updatePromotion(UUID promotionId, SA.irms.ordering.application.command.MenuCommands.PromotionUpsert request) {
        PromotionDiscountParser.ParsedDiscount parsedDiscount = discountParser.parseDiscount(request.discount());
        if (!repository.updatePromotion(
                promotionId,
                request.code(),
                request.code() + " Promotion",
                parsedDiscount.type(),
                parsedDiscount.value(),
                discountParser.parseDate(request.validUntil()) == null ? null : java.sql.Timestamp.from(discountParser.parseDate(request.validUntil()))
        )) {
            throw new NotFoundException("Promotion was not found.");
        }
        publishPromotionUpdated(promotionId, request, "updated");
        return new SA.irms.ordering.application.view.MenuViews.PromotionView(promotionId, request.code(), request.discount(), request.validUntil(), true);
    }

    @Transactional
    void deletePromotion(UUID promotionId) {
        if (!repository.deletePromotion(promotionId)) {
            throw new NotFoundException("Promotion was not found.");
        }
        outboxPublisher.publish(new PromotionUpdatedEvent(promotionId.toString(), Map.of(
                "promotionId", promotionId.toString(),
                "changeType", "deleted",
                "active", false
        )), "promotion-" + promotionId, null);
    }

    BigDecimal applyPromotion(UUID billId, String code) {
        OrderRepository.PromotionRow promotion = repository.findActivePromotionByCode(code)
                .orElseThrow(() -> new NotFoundException("Promotion code was not found or is inactive."));
        BigDecimal subTotal = repository.findBillSubtotal(billId)
                .orElseThrow(() -> new NotFoundException("Bill was not found."));
        BigDecimal discount = "percentage".equals(promotion.type())
                ? subTotal.multiply(promotion.value()).divide(BigDecimal.valueOf(100))
                : promotion.value();
        repository.applyPromotionToBill(billId, promotion.id(), code, discount);
        return discount;
    }

    private void publishPromotionUpdated(UUID promotionId, SA.irms.ordering.application.command.MenuCommands.PromotionUpsert request, String changeType) {
        outboxPublisher.publish(new PromotionUpdatedEvent(promotionId.toString(), Map.of(
                "promotionId", promotionId.toString(),
                "code", request.code(),
                "discount", request.discount(),
                "validUntil", request.validUntil() == null ? "" : request.validUntil(),
                "changeType", changeType,
                "active", true
        )), "promotion-" + promotionId, null);
    }
}
