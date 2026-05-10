package SA.irms.ordering.application;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.error.ConflictException;
import SA.irms.common.error.NotFoundException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.ordering.application.port.out.OrderQueryRepository;
import SA.irms.ordering.application.port.out.OrderRepository;
import SA.irms.ordering.domain.OrderItemStatusPolicy;
import SA.irms.ordering.application.events.OrderCancelledEvent;
import SA.irms.common.outbox.DomainEventPublisher;
import SA.irms.common.context.RequestMetadata;
import SA.irms.ordering.application.KitchenCoordinationPort.KitchenOrderItemCommand;

@Service
class OrderItemWorkflowService {
    private final AuditRecorder auditService;
    private final KitchenCoordinationPort kitchenCoordinationPort;
    private final OrderStateCoordinator orderStateCoordinator;
    private final OrderItemStatusPolicy orderItemStatusPolicy;
    private final DomainEventPublisher outboxPublisher;
    private final OrderRepository repository;
    private final OrderQueryRepository queryRepository;

    OrderItemWorkflowService(AuditRecorder auditService, KitchenCoordinationPort kitchenCoordinationPort,
                             OrderStateCoordinator orderStateCoordinator, OrderItemStatusPolicy orderItemStatusPolicy,
                             DomainEventPublisher outboxPublisher, OrderRepository repository, OrderQueryRepository queryRepository) {
        this.auditService = auditService;
        this.kitchenCoordinationPort = kitchenCoordinationPort;
        this.orderStateCoordinator = orderStateCoordinator;
        this.orderItemStatusPolicy = orderItemStatusPolicy;
        this.outboxPublisher = outboxPublisher;
        this.repository = repository;
        this.queryRepository = queryRepository;
    }

    @Transactional
    public SA.irms.ordering.application.view.OrderViews.OrderedItemView markServed(UUID orderItemId, UUID actorUserId, String correlationId) {
        OrderRepository.OrderItemServedState state = repository.findServedState(orderItemId)
                .orElseThrow(() -> new NotFoundException("Order item was not found."));
        if ("served".equals(state.status())) {
            return findOrderItem(orderItemId);
        }
        repository.markOrderItemServed(orderItemId);
        orderStateCoordinator.refreshOrderForItem(orderItemId);
        return findOrderItem(orderItemId);
    }

    @Transactional
    public SA.irms.ordering.application.view.OrderViews.OrderedItemView markDelayed(UUID orderItemId) {
        repository.markOrderItemDelayed(orderItemId);
        kitchenCoordinationPort.holdOrderItemForService(orderItemId);
        orderStateCoordinator.refreshOrderForItem(orderItemId);
        return findOrderItem(orderItemId);
    }

    @Transactional
    public SA.irms.ordering.application.view.OrderViews.OrderedItemView sendDelayedItem(UUID orderItemId, AuthenticatedUser actor, String correlationId, RequestMetadata httpServletRequest) {
        OrderQueryRepository.OrderItemRoutingRow item = queryRepository.findOrderItemRouting(orderItemId)
                .orElseThrow(() -> new NotFoundException("Order item was not found."));
        orderItemStatusPolicy.ensureDelayedItemCanBeSent(item.orderStatus(), item.status());
        kitchenCoordinationPort.releaseHeldOrderItem(item.orderId(), new KitchenOrderItemCommand(orderItemId, item.station(), item.quantity()),
                "Released delayed item to kitchen.");
        repository.markOrderItemSentToKitchen(orderItemId);
        auditService.record(actor.userId(), "order.item.sent_to_kitchen", "OrderItem", orderItemId.toString(), correlationId, null, false,
                httpServletRequest.remoteIp(), Map.of("status", item.status()), Map.of("status", "sent_to_kitchen"));
        orderStateCoordinator.refreshOrderForItem(orderItemId);
        return findOrderItem(orderItemId);
    }

    @Transactional
    public SA.irms.ordering.application.view.OrderViews.OrderedItemView cancelOrderItem(UUID orderItemId, String reason, AuthenticatedUser actor, String correlationId, RequestMetadata httpServletRequest) {
        SA.irms.ordering.application.view.OrderViews.OrderedItemView existing = findOrderItem(orderItemId);
        orderItemStatusPolicy.ensureItemCanBeCancelled(existing.status(), actor);
        repository.cancelOrderItem(orderItemId, reason);
        kitchenCoordinationPort.blockOrderItem(orderItemId, reason);
        orderStateCoordinator.refreshOrderForItem(orderItemId);
        maybePublishOrderCancelled(existing.orderId(), reason, correlationId, "order.item.cancelled");
        auditService.record(actor.userId(), "order.item.cancelled", "OrderItem", orderItemId.toString(), correlationId, reason, false,
                httpServletRequest.remoteIp(), Map.of("status", existing.status()), Map.of("status", "cancelled"));
        return findOrderItem(orderItemId);
    }

    @Transactional
    public void cancelOrder(UUID orderId, String reason, AuthenticatedUser actor, String correlationId, RequestMetadata httpServletRequest) {
        String currentStatus = repository.findOrderStatus(orderId)
                .orElseThrow(() -> new NotFoundException("Order was not found."));
        if ("cancelled".equals(currentStatus) || "completed".equals(currentStatus)) {
            throw new ConflictException("Order cannot be cancelled from status " + currentStatus + ".");
        }
        int affected = repository.cancelActiveOrderItems(orderId, reason);
        repository.markOrderCancelled(orderId);
        outboxPublisher.publish(new OrderCancelledEvent(orderId.toString(), Map.of(
                "orderId", orderId.toString(),
                "reason", reason == null ? "Order cancelled" : reason,
                "cancelledItems", affected,
                "previousStatus", currentStatus
        )), correlationId, null);
        auditService.record(actor.userId(), "order.cancelled", "Order", orderId.toString(), correlationId, reason, false,
                httpServletRequest.remoteIp(), Map.of("status", currentStatus), Map.of("status", "cancelled", "cancelledItems", affected));
    }

    private void maybePublishOrderCancelled(UUID orderId, String reason, String correlationId, String cause) {
        String orderStatus = repository.findOrderStatus(orderId).orElse(null);
        if (!"cancelled".equals(orderStatus)) {
            return;
        }
        outboxPublisher.publish(new OrderCancelledEvent(orderId.toString(), Map.of(
                "orderId", orderId.toString(),
                "reason", reason == null ? "Order cancelled" : reason,
                "cause", cause
        )), correlationId, null);
    }

    private SA.irms.ordering.application.view.OrderViews.OrderedItemView findOrderItem(UUID orderItemId) {
        return queryRepository.loadOrderedItemView(orderItemId);
    }
}
