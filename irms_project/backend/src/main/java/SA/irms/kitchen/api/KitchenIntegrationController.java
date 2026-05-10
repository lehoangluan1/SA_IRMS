package SA.irms.kitchen.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.kitchen.application.KitchenOrderItemCommand;
import SA.irms.kitchen.application.KitchenOrderRoutingService;

@RestController
@RequestMapping("/internal/kitchen")
class KitchenIntegrationController {
    private final KitchenOrderRoutingService routingService;

    KitchenIntegrationController(KitchenOrderRoutingService routingService) {
        this.routingService = routingService;
    }

    @PostMapping("/orders/{orderId}/tickets")
    ResponseEntity<Void> queueConfirmedItems(@PathVariable UUID orderId, @RequestBody QueueItemsRequest request) {
        routingService.queueConfirmedItems(orderId, request.items(), request.notes());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/order-items/{orderItemId}/hold")
    ResponseEntity<Void> holdOrderItemForService(@PathVariable UUID orderItemId) {
        routingService.holdOrderItemForService(orderItemId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/orders/{orderId}/items/release")
    ResponseEntity<Void> releaseHeldOrderItem(@PathVariable UUID orderId, @RequestBody ReleaseItemRequest request) {
        routingService.releaseHeldOrderItem(orderId, request.item(), request.notes());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/order-items/{orderItemId}/block")
    ResponseEntity<Void> blockOrderItem(@PathVariable UUID orderItemId, @RequestBody BlockItemRequest request) {
        routingService.blockOrderItem(orderItemId, request.reason());
        return ResponseEntity.accepted().build();
    }

    record QueueItemsRequest(List<KitchenOrderItemCommand> items, String notes) {
    }

    record ReleaseItemRequest(KitchenOrderItemCommand item, String notes) {
    }

    record BlockItemRequest(String reason) {
    }
}
