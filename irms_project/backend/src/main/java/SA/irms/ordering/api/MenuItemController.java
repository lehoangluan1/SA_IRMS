package SA.irms.ordering.api;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.api.EntityReferenceResponse;
import SA.irms.common.security.PermissionGuard;
import SA.irms.common.web.RequestContext;
import SA.irms.ordering.application.MenuService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/menu/items")
public class MenuItemController {
    private final MenuService menuService;
    private final PermissionGuard permissionGuard;

    public MenuItemController(MenuService menuService, PermissionGuard permissionGuard) {
        this.menuService = menuService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiEnvelope<List<SA.irms.ordering.application.view.MenuViews.MenuItemView>> items(HttpServletRequest request) {
        permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.loadItems(), RequestContext.getCorrelationId(request));
    }

    @GetMapping("/{menuItemId}")
    public ApiEnvelope<SA.irms.ordering.application.view.MenuViews.MenuItemView> item(@PathVariable UUID menuItemId, HttpServletRequest request) {
        permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.findItem(menuItemId), RequestContext.getCorrelationId(request));
    }

    @PostMapping
    public ApiEnvelope<SA.irms.ordering.application.view.MenuViews.MenuItemView> createItem(@Valid @RequestBody MenuItemBody requestBody, HttpServletRequest request) {
        var actor = permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.createItem(requestBody.toUpsert(), actor.userId(), RequestContext.getCorrelationId(request), RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    @PutMapping("/{menuItemId}")
    public ApiEnvelope<SA.irms.ordering.application.view.MenuViews.MenuItemView> updateItem(@PathVariable UUID menuItemId, @Valid @RequestBody MenuItemBody requestBody, HttpServletRequest request) {
        var actor = permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.updateItem(menuItemId, requestBody.toUpsert(), actor.userId(), RequestContext.getCorrelationId(request), RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    @PatchMapping("/{menuItemId}/availability")
    public ApiEnvelope<SA.irms.ordering.application.view.MenuViews.MenuItemView> toggleAvailability(@PathVariable UUID menuItemId, HttpServletRequest request) {
        var actor = permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.toggleAvailability(menuItemId, actor.userId(), RequestContext.getCorrelationId(request), RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    @DeleteMapping("/{menuItemId}")
    public ApiEnvelope<EntityReferenceResponse> deleteItem(@PathVariable UUID menuItemId, HttpServletRequest request) {
        permissionGuard.require("menu.manage");
        menuService.deleteItem(menuItemId);
        return ApiEnvelope.of(new EntityReferenceResponse("menuItem", menuItemId), RequestContext.getCorrelationId(request));
    }

    public record MenuItemBody(@NotBlank String name, @NotBlank String description, @NotBlank String category, @NotNull BigDecimal price, @NotBlank String station, java.util.List<String> allergens, java.util.List<String> ingredients, Integer preparationTimeMin) {
        SA.irms.ordering.application.command.MenuCommands.MenuItemUpsert toUpsert() {
            return new SA.irms.ordering.application.command.MenuCommands.MenuItemUpsert(name, description, category, price, station, allergens, ingredients, preparationTimeMin);
        }
    }
}
