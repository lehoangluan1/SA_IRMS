package SA.irms.ordering.api;

import java.util.UUID;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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

@RestController
@RequestMapping("/api/menu/promotions")
public class MenuPromotionController {
    private final MenuService menuService;
    private final PermissionGuard permissionGuard;

    public MenuPromotionController(MenuService menuService, PermissionGuard permissionGuard) {
        this.menuService = menuService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiEnvelope<List<SA.irms.ordering.application.view.MenuViews.PromotionView>> promotions(HttpServletRequest request) {
        permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.loadPromotions(), RequestContext.getCorrelationId(request));
    }

    @GetMapping("/{promotionId}")
    public ApiEnvelope<SA.irms.ordering.application.view.MenuViews.PromotionView> promotion(@PathVariable UUID promotionId, HttpServletRequest request) {
        permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.findPromotion(promotionId), RequestContext.getCorrelationId(request));
    }

    @PostMapping
    public ApiEnvelope<SA.irms.ordering.application.view.MenuViews.PromotionView> createPromotion(@Valid @RequestBody PromotionBody requestBody, HttpServletRequest request) {
        permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.createPromotion(requestBody.toUpsert()), RequestContext.getCorrelationId(request));
    }

    @PutMapping("/{promotionId}")
    public ApiEnvelope<SA.irms.ordering.application.view.MenuViews.PromotionView> updatePromotion(@PathVariable UUID promotionId, @Valid @RequestBody PromotionBody requestBody, HttpServletRequest request) {
        permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.updatePromotion(promotionId, requestBody.toUpsert()), RequestContext.getCorrelationId(request));
    }

    @DeleteMapping("/{promotionId}")
    public ApiEnvelope<EntityReferenceResponse> deletePromotion(@PathVariable UUID promotionId, HttpServletRequest request) {
        permissionGuard.require("menu.manage");
        menuService.deletePromotion(promotionId);
        return ApiEnvelope.of(new EntityReferenceResponse("promotion", promotionId), RequestContext.getCorrelationId(request));
    }

    public record PromotionBody(@NotBlank String code, @NotBlank String discount, String validUntil) {
        SA.irms.ordering.application.command.MenuCommands.PromotionUpsert toUpsert() {
            return new SA.irms.ordering.application.command.MenuCommands.PromotionUpsert(code, discount, validUntil);
        }
    }
}
