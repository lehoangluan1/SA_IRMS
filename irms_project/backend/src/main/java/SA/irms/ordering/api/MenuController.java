package SA.irms.ordering.api;

import java.util.UUID;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.security.PermissionGuard;
import SA.irms.common.web.RequestContext;
import SA.irms.ordering.application.MenuService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/menu")
public class MenuController {
    private final MenuService menuService;
    private final PermissionGuard permissionGuard;

    public MenuController(MenuService menuService, PermissionGuard permissionGuard) {
        this.menuService = menuService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping
    public ApiEnvelope<SA.irms.ordering.application.view.MenuViews.MenuOverview> overview(HttpServletRequest request) {
        permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.load(), RequestContext.getCorrelationId(request));
    }

    @GetMapping("/combos")
    public ApiEnvelope<List<SA.irms.ordering.application.view.MenuViews.ComboView>> combos(HttpServletRequest request) {
        permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.loadCombos(), RequestContext.getCorrelationId(request));
    }

    @GetMapping("/combos/{comboId}")
    public ApiEnvelope<SA.irms.ordering.application.view.MenuViews.ComboView> combo(@PathVariable UUID comboId, HttpServletRequest request) {
        permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.findCombo(comboId), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/combos")
    public ApiEnvelope<SA.irms.ordering.application.view.MenuViews.ComboView> createCombo(@RequestBody SA.irms.ordering.application.command.MenuCommands.ComboUpsert requestBody, HttpServletRequest request) {
        permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.createCombo(requestBody), RequestContext.getCorrelationId(request));
    }

    @PutMapping("/combos/{comboId}")
    public ApiEnvelope<SA.irms.ordering.application.view.MenuViews.ComboView> updateCombo(
            @PathVariable UUID comboId,
            @RequestBody SA.irms.ordering.application.command.MenuCommands.ComboUpsert requestBody,
            HttpServletRequest request
    ) {
        permissionGuard.require("menu.manage");
        return ApiEnvelope.of(menuService.updateCombo(comboId, requestBody), RequestContext.getCorrelationId(request));
    }
}
