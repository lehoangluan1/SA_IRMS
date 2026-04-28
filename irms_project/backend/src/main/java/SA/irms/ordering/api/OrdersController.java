package SA.irms.ordering.api;

import java.util.UUID;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import SA.irms.common.api.ApiEnvelope;
import SA.irms.common.api.EntityStatusResponse;
import SA.irms.common.security.PermissionGuard;
import SA.irms.common.web.RequestContext;
import SA.irms.ordering.application.OrdersService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/orders")
public class OrdersController {
    private final OrdersService ordersService;
    private final PermissionGuard permissionGuard;

    public OrdersController(OrdersService ordersService, PermissionGuard permissionGuard) {
        this.ordersService = ordersService;
        this.permissionGuard = permissionGuard;
    }

    @GetMapping("/overview")
    public ApiEnvelope<SA.irms.ordering.application.view.OrderViews.OrdersOverview> overview(
            @RequestParam(value = "sessionId", required = false) UUID sessionId,
            HttpServletRequest request
    ) {
        permissionGuard.require("orders.create");
        return ApiEnvelope.of(ordersService.load(sessionId), RequestContext.getCorrelationId(request));
    }

    @PostMapping
    public ApiEnvelope<SA.irms.ordering.application.view.OrderViews.OrdersOverview> createOrder(
            @Valid @RequestBody CreateOrderBody requestBody,
            HttpServletRequest request
    ) {
        var actor = permissionGuard.require("orders.send");
        return ApiEnvelope.of(ordersService.createOrder(requestBody.toRequest(), actor, RequestContext.getCorrelationId(request), RequestContext.metadata(request)),
                RequestContext.getCorrelationId(request));
    }

    @PostMapping("/{orderId}/confirm")
    public ApiEnvelope<SA.irms.ordering.application.view.OrderViews.OrdersOverview> confirmDraftOrder(@PathVariable UUID orderId, HttpServletRequest request) {
        var actor = permissionGuard.require("orders.send");
        return ApiEnvelope.of(
                ordersService.confirmDraftOrder(orderId, actor, RequestContext.getCorrelationId(request), RequestContext.metadata(request)),
                RequestContext.getCorrelationId(request)
        );
    }

    @PostMapping("/{orderId}/cancel")
    public ApiEnvelope<EntityStatusResponse> cancelOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody CancelBody requestBody,
            HttpServletRequest request
    ) {
        var actor = permissionGuard.require("orders.edit");
        ordersService.cancelOrder(orderId, requestBody.reason(), actor, RequestContext.getCorrelationId(request), RequestContext.metadata(request));
        return ApiEnvelope.of(new EntityStatusResponse("order", orderId, "cancelled"), RequestContext.getCorrelationId(request));
    }

    @PatchMapping("/items/{orderItemId}/served")
    public ApiEnvelope<SA.irms.ordering.application.view.OrderViews.OrderedItemView> markServed(@PathVariable UUID orderItemId, HttpServletRequest request) {
        var actor = permissionGuard.require("orders.edit");
        return ApiEnvelope.of(ordersService.markServed(orderItemId, actor.userId(), RequestContext.getCorrelationId(request)),
                RequestContext.getCorrelationId(request));
    }

    @PatchMapping("/items/{orderItemId}/delayed")
    public ApiEnvelope<SA.irms.ordering.application.view.OrderViews.OrderedItemView> markDelayed(@PathVariable UUID orderItemId, HttpServletRequest request) {
        permissionGuard.require("orders.edit");
        return ApiEnvelope.of(ordersService.markDelayed(orderItemId), RequestContext.getCorrelationId(request));
    }

    @PostMapping("/items/{orderItemId}/send")
    public ApiEnvelope<SA.irms.ordering.application.view.OrderViews.OrderedItemView> sendDelayedItem(@PathVariable UUID orderItemId, HttpServletRequest request) {
        var actor = permissionGuard.require("orders.send");
        return ApiEnvelope.of(ordersService.sendDelayedItem(orderItemId, actor, RequestContext.getCorrelationId(request), RequestContext.metadata(request)),
                RequestContext.getCorrelationId(request));
    }

    @PostMapping("/items/{orderItemId}/cancel")
    public ApiEnvelope<SA.irms.ordering.application.view.OrderViews.OrderedItemView> cancelItem(
            @PathVariable UUID orderItemId,
            @Valid @RequestBody CancelBody requestBody,
            HttpServletRequest request
    ) {
        var actor = permissionGuard.require("orders.edit");
        return ApiEnvelope.of(ordersService.cancelOrderItem(orderItemId, requestBody.reason(), actor,
                RequestContext.getCorrelationId(request), RequestContext.metadata(request)), RequestContext.getCorrelationId(request));
    }

    public record CreateOrderBody(
            @NotNull UUID sessionId,
            String specialInstructions,
            java.util.List<@Valid CreateOrderItemBody> items,
            java.util.List<@Valid ComboSelectionBody> comboSelections,
            Boolean draft
    ) {
        SA.irms.ordering.application.command.OrderCommands.CreateOrderRequest toRequest() {
            return new SA.irms.ordering.application.command.OrderCommands.CreateOrderRequest(
                    sessionId,
                    specialInstructions,
                    items == null ? java.util.List.of() : items.stream().map(CreateOrderItemBody::toRequest).toList(),
                    comboSelections == null ? java.util.List.of() : comboSelections.stream().map(ComboSelectionBody::toRequest).toList(),
                    Boolean.TRUE.equals(draft)
            );
        }
    }

    public record CreateOrderItemBody(
            @NotNull UUID menuItemId,
            @NotNull Integer quantity,
            String note,
            java.util.List<UUID> modifierOptionIds,
            Boolean sendLater
    ) {
        SA.irms.ordering.application.command.OrderCommands.OrderItemRequest toRequest() {
            return new SA.irms.ordering.application.command.OrderCommands.OrderItemRequest(
                    menuItemId,
                    quantity == null ? 0 : quantity,
                    note,
                    modifierOptionIds == null ? java.util.List.of() : modifierOptionIds,
                    Boolean.TRUE.equals(sendLater)
            );
        }
    }

    public record ComboSelectionBody(
            @NotNull UUID comboId,
            @NotNull Integer quantity,
            String allergyNotes,
            String specialInstructions,
            java.util.List<@Valid ComboGroupSelectionBody> groups
    ) {
        SA.irms.ordering.application.command.OrderCommands.ComboSelectionRequest toRequest() {
            return new SA.irms.ordering.application.command.OrderCommands.ComboSelectionRequest(
                    comboId,
                    quantity == null ? 0 : quantity,
                    allergyNotes,
                    specialInstructions,
                    groups == null ? java.util.List.of() : groups.stream().map(ComboGroupSelectionBody::toRequest).toList()
            );
        }
    }

    public record ComboGroupSelectionBody(@NotNull UUID comboGroupId, java.util.List<UUID> selectedOptionIds) {
        SA.irms.ordering.application.command.OrderCommands.ComboGroupSelectionRequest toRequest() {
            return new SA.irms.ordering.application.command.OrderCommands.ComboGroupSelectionRequest(
                    comboGroupId,
                    selectedOptionIds == null ? java.util.List.of() : selectedOptionIds
            );
        }
    }

    public record CancelBody(String reason) {
    }
}
