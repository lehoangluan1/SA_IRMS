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
@RequestMapping("/api/menu/categories")
public class MenuCategoryController {
    private final MenuService menuService;
    private final PermissionGuard permissionGuard;

    public MenuCategoryController(MenuService menuService, PermissionGuard permissionGuard) {
        this.menuService = menuService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiEnvelope<List<SA.irms.ordering.application.view.MenuViews.CategoryView>> categories(HttpServletRequest request) {
        permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.loadCategories(), RequestContext.getCorrelationId(request));
    }

    @GetMapping("/{categoryId}")
    public ApiEnvelope<SA.irms.ordering.application.view.MenuViews.CategoryView> category(@PathVariable UUID categoryId, HttpServletRequest request) {
        permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.findCategory(categoryId), RequestContext.getCorrelationId(request));
    }

    @PostMapping
    public ApiEnvelope<SA.irms.ordering.application.view.MenuViews.CategoryView> createCategory(@Valid @RequestBody CategoryBody requestBody, HttpServletRequest request) {
        permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.createCategory(requestBody.name()), RequestContext.getCorrelationId(request));
    }

    @PutMapping("/{categoryId}")
    public ApiEnvelope<SA.irms.ordering.application.view.MenuViews.CategoryView> updateCategory(@PathVariable UUID categoryId, @Valid @RequestBody CategoryBody requestBody, HttpServletRequest request) {
        permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.updateCategory(categoryId, requestBody.name()), RequestContext.getCorrelationId(request));
    }

    @DeleteMapping("/{categoryId}")
    public ApiEnvelope<EntityReferenceResponse> deleteCategory(@PathVariable UUID categoryId, HttpServletRequest request) {
        permissionGuard.require("menu.manage");
        menuService.deleteCategory(categoryId);
        return ApiEnvelope.of(new EntityReferenceResponse("menuCategory", categoryId), RequestContext.getCorrelationId(request));
    }

    public record CategoryBody(@NotBlank String name) {
    }
}
