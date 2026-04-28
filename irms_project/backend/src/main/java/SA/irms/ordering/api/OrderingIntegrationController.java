package SA.irms.ordering.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.ordering.application.MenuService;
import SA.irms.ordering.application.OrderStateCoordinator;
import SA.irms.ordering.application.OrderItemLineStatusUpdate;

@RestController
@RequestMapping("/internal/ordering")
class OrderingIntegrationController {
    private final OrderStateCoordinator orderStateCoordinator;
    private final MenuService menuService;

    OrderingIntegrationController(OrderStateCoordinator orderStateCoordinator, MenuService menuService) {
        this.orderStateCoordinator = orderStateCoordinator;
        this.menuService = menuService;
    }

    @PostMapping("/orders/{orderId}/refresh-status")
    ResponseEntity<Void> refreshOrder(@PathVariable UUID orderId) {
        orderStateCoordinator.refreshOrder(orderId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/orders/{orderId}/line-statuses")
    ResponseEntity<Void> applyKitchenLineStatuses(@PathVariable UUID orderId, @RequestBody LineStatusRequest request) {
        orderStateCoordinator.applyKitchenLineStatuses(orderId, request.items());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/promotions/apply")
    ResponseEntity<PromotionResponse> applyPromotion(@RequestBody PromotionRequest request) {
        BigDecimal discount = menuService.applyPromotion(request.billId(), request.code());
        return ResponseEntity.ok(new PromotionResponse(discount));
    }

    record LineStatusRequest(List<OrderItemLineStatusUpdate> items) {
    }

    record PromotionRequest(UUID billId, String code) {
    }

    record PromotionResponse(BigDecimal discount) {
    }
}
