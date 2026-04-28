package SA.irms.kitchen.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.security.PermissionGuard;
import SA.irms.common.web.RequestContext;
import SA.irms.kitchen.application.KitchenService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/kitchen")
public class KitchenController {
    private final KitchenService kitchenService;
    private final PermissionGuard permissionGuard;

    public KitchenController(KitchenService kitchenService, PermissionGuard permissionGuard) {
        this.kitchenService = kitchenService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/overview")
    public ApiEnvelope<SA.irms.kitchen.application.view.KitchenViews.KitchenOverview> overview(
            @RequestParam(value = "station", required = false) String station,
            HttpServletRequest request
    ) {
        permissionGuard.require("kitchen.manage");
        return ApiEnvelope.of(kitchenService.load(station), RequestContext.getCorrelationId(request));
    }

    @PatchMapping("/tickets/{ticketId}/status")
    public ApiEnvelope<SA.irms.kitchen.application.view.KitchenViews.TicketView> updateTicketStatus(
            @PathVariable UUID ticketId,
            @RequestBody StatusBody requestBody,
            HttpServletRequest request
    ) {
        var actor = permissionGuard.require("kitchen.manage");
        return ApiEnvelope.of(
                kitchenService.updateTicketStatus(ticketId, requestBody.status(), actor, RequestContext.getCorrelationId(request)),
                RequestContext.getCorrelationId(request)
        );
    }

    @PatchMapping("/items/{ticketItemId}/status")
    public ApiEnvelope<SA.irms.kitchen.application.view.KitchenViews.TicketView> updateItemStatus(
            @PathVariable UUID ticketItemId,
            @RequestBody StatusBody requestBody,
            HttpServletRequest request
    ) {
        var actor = permissionGuard.require("kitchen.manage");
        return ApiEnvelope.of(kitchenService.updateItemStatus(
                        ticketItemId,
                        requestBody.status(),
                        requestBody.reason(),
                        actor,
                        RequestContext.getCorrelationId(request),
                        RequestContext.metadata(request)
                ),
                RequestContext.getCorrelationId(request));
    }

    @PostMapping("/tickets/{ticketId}/priority")
    public ApiEnvelope<SA.irms.kitchen.application.view.KitchenViews.TicketView> togglePriority(
            @PathVariable UUID ticketId,
            @RequestBody StatusBody requestBody,
            HttpServletRequest request
    ) {
        var actor = permissionGuard.require("kitchen.manage");
        return ApiEnvelope.of(kitchenService.togglePriority(ticketId, requestBody.reason(), actor,
                        RequestContext.getCorrelationId(request), RequestContext.metadata(request)),
                RequestContext.getCorrelationId(request));
    }

    @PostMapping("/tickets/{ticketId}/cancel")
    public ApiEnvelope<SA.irms.kitchen.application.view.KitchenViews.TicketView> cancelTicket(
            @PathVariable UUID ticketId,
            @RequestBody StatusBody requestBody,
            HttpServletRequest request
    ) {
        var actor = permissionGuard.require("kitchen.manage");
        return ApiEnvelope.of(kitchenService.cancelTicket(ticketId, requestBody.reason(), actor,
                RequestContext.getCorrelationId(request), RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/tickets/{ticketId}/serve")
    public ApiEnvelope<SA.irms.kitchen.application.view.KitchenViews.TicketView> serveTicket(@PathVariable UUID ticketId, HttpServletRequest request) {
        var actor = permissionGuard.require("kitchen.manage");
        return ApiEnvelope.of(kitchenService.serveTicket(ticketId, actor, RequestContext.getCorrelationId(request)),
                RequestContext.getCorrelationId(request));
    }

    @PostMapping("/tickets/{ticketId}/return")
    public ApiEnvelope<SA.irms.kitchen.application.view.KitchenViews.TicketView> returnTicket(
            @PathVariable UUID ticketId,
            @RequestBody StatusBody requestBody,
            HttpServletRequest request
    ) {
        var actor = permissionGuard.require("kitchen.manage");
        return ApiEnvelope.of(kitchenService.returnTicket(ticketId, requestBody.reason(), actor, RequestContext.getCorrelationId(request), RequestContext.metadata(request)),
                RequestContext.getCorrelationId(request));
    }

    public record StatusBody(String status, String reason) {
    }
}
