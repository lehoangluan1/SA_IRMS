package SA.irms.inventory.api;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.api.EntityReferenceResponse;
import SA.irms.common.api.EntityStatusResponse;
import SA.irms.common.security.PermissionGuard;
import SA.irms.common.web.RequestContext;
import SA.irms.inventory.application.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;
    private final PermissionGuard permissionGuard;

    public InventoryController(InventoryService inventoryService, PermissionGuard permissionGuard) {
        this.inventoryService = inventoryService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiEnvelope<SA.irms.inventory.application.view.InventoryViews.InventoryOverview> inventory(HttpServletRequest request) {
        permissionGuard.require("inventory.manage");
        return ApiEnvelope.of(inventoryService.load(), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/items")
    public ApiEnvelope<SA.irms.inventory.application.view.InventoryViews.IngredientView> createIngredient(
            @Valid @RequestBody InventoryItemBody requestBody,
            HttpServletRequest request
    ) {
        permissionGuard.require("inventory.manage");
        return ApiEnvelope.of(inventoryService.createIngredient(requestBody.toUpsert()), RequestContext.getCorrelationId(request));
    }

    @PutMapping("/items/{inventoryItemId}")
    public ApiEnvelope<SA.irms.inventory.application.view.InventoryViews.IngredientView> updateIngredient(
            @PathVariable UUID inventoryItemId,
            @Valid @RequestBody InventoryItemBody requestBody,
            HttpServletRequest request
    ) {
        var actor = permissionGuard.require("inventory.manage");
        return ApiEnvelope.of(inventoryService.updateIngredient(inventoryItemId, requestBody.toUpsert(), actor.userId(),
                RequestContext.getCorrelationId(request), RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    @DeleteMapping("/items/{inventoryItemId}")
    public ApiEnvelope<EntityReferenceResponse> deleteIngredient(@PathVariable UUID inventoryItemId, HttpServletRequest request) {
        permissionGuard.require("inventory.manage");
        inventoryService.deleteIngredient(inventoryItemId);
        return ApiEnvelope.of(new EntityReferenceResponse("inventoryItem", inventoryItemId), RequestContext.getCorrelationId(request));
    }

    @PatchMapping("/alerts/{alertId}/acknowledge")
    public ApiEnvelope<EntityStatusResponse> acknowledgeAlert(@PathVariable UUID alertId, HttpServletRequest request) {
        var actor = permissionGuard.require("inventory.manage");
        inventoryService.acknowledgeAlert(alertId, actor.userId());
        return ApiEnvelope.of(new EntityStatusResponse("inventoryAlert", alertId, "acknowledged"), RequestContext.getCorrelationId(request));
    }

    public record InventoryItemBody(
            @NotBlank String name,
            @NotBlank String unit,
            @NotNull BigDecimal current,
            @NotNull BigDecimal minimum,
            @NotNull BigDecimal maximum,
            @NotNull BigDecimal cost,
            @NotBlank String category
    ) {
        SA.irms.inventory.application.command.InventoryCommands.InventoryUpsert toUpsert() {
            return new SA.irms.inventory.application.command.InventoryCommands.InventoryUpsert(name, unit, current, minimum, maximum, cost, category);
        }
    }
}
