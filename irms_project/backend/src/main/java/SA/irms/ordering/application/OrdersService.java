package SA.irms.ordering.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.security.AuthenticatedUser;
import SA.irms.common.context.RequestMetadata;

@Service
public class OrdersService {
    private final OrdersReadService ordersReadService;
    private final OrderCreationService orderCreationService;
    private final OrderDraftConfirmationService orderDraftConfirmationService;
    private final OrderItemWorkflowService orderItemWorkflowService;

    public OrdersService(OrdersReadService ordersReadService, OrderCreationService orderCreationService,
            OrderDraftConfirmationService orderDraftConfirmationService, OrderItemWorkflowService orderItemWorkflowService) {
        this.ordersReadService = ordersReadService;
        this.orderCreationService = orderCreationService;
        this.orderDraftConfirmationService = orderDraftConfirmationService;
        this.orderItemWorkflowService = orderItemWorkflowService;
    }

    public SA.irms.ordering.application.view.OrderViews.OrdersOverview load(UUID sessionId) { return ordersReadService.load(sessionId); }
    public SA.irms.ordering.application.view.OrderViews.OrdersOverview createOrder(SA.irms.ordering.application.command.OrderCommands.CreateOrderRequest request, AuthenticatedUser actor, String correlationId, RequestMetadata httpServletRequest) {
        return orderCreationService.createOrder(request, actor, correlationId, httpServletRequest);
    }
    public SA.irms.ordering.application.view.OrderViews.OrdersOverview confirmDraftOrder(UUID orderId, AuthenticatedUser actor, String correlationId, RequestMetadata httpServletRequest) {
        return orderDraftConfirmationService.confirmDraftOrder(orderId, actor, correlationId, httpServletRequest);
    }
    @Transactional public SA.irms.ordering.application.view.OrderViews.OrderedItemView markServed(UUID orderItemId, UUID actorUserId, String correlationId) { return orderItemWorkflowService.markServed(orderItemId, actorUserId, correlationId); }
    @Transactional public SA.irms.ordering.application.view.OrderViews.OrderedItemView markDelayed(UUID orderItemId) { return orderItemWorkflowService.markDelayed(orderItemId); }
    @Transactional public SA.irms.ordering.application.view.OrderViews.OrderedItemView sendDelayedItem(UUID orderItemId, AuthenticatedUser actor, String correlationId, RequestMetadata request) { return orderItemWorkflowService.sendDelayedItem(orderItemId, actor, correlationId, request); }
    @Transactional public SA.irms.ordering.application.view.OrderViews.OrderedItemView cancelOrderItem(UUID orderItemId, String reason, AuthenticatedUser actor, String correlationId, RequestMetadata request) { return orderItemWorkflowService.cancelOrderItem(orderItemId, reason, actor, correlationId, request); }
    @Transactional public void cancelOrder(UUID orderId, String reason, AuthenticatedUser actor, String correlationId, RequestMetadata request) { orderItemWorkflowService.cancelOrder(orderId, reason, actor, correlationId, request); }

}
