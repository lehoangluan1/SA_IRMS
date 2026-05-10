package SA.irms.inventory.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.inventory.application.InventoryService;

@RestController
@RequestMapping("/internal/inventory")
class InventoryIntegrationController {
    private final InventoryService inventoryService;

    InventoryIntegrationController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/consumption/kitchen-start")
    ResponseEntity<Void> consumeForKitchenStart(@RequestBody KitchenStartConsumptionRequest request) {
        inventoryService.consumeForKitchenStart(request.orderItemId(), request.actorUserId(), request.correlationId());
        return ResponseEntity.accepted().build();
    }

    record KitchenStartConsumptionRequest(UUID orderItemId, UUID actorUserId, String correlationId) {
    }
}
