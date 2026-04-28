package SA.irms.ordering.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.error.ConflictException;
import SA.irms.ordering.application.port.out.OrderQueryRepository;
import SA.irms.ordering.application.port.out.OrderRepository;
import SA.irms.ordering.domain.OrderStatusPolicy;
import SA.irms.ordering.application.OrderItemLineStatusUpdate;

@Service
public class OrderStateCoordinator {
    private static final List<String> KITCHEN_CONTROLLED_STATUSES = List.of("sent_to_kitchen", "cooking", "ready", "blocked", "hold_for_service", "served");

    private final OrderQueryRepository queryRepository;
    private final OrderRepository repository;
    private final OrderStatusPolicy orderStatusPolicy;

    public OrderStateCoordinator(OrderQueryRepository queryRepository, OrderRepository repository, OrderStatusPolicy orderStatusPolicy) {
        this.queryRepository = queryRepository;
        this.repository = repository;
        this.orderStatusPolicy = orderStatusPolicy;
    }

    public void refreshOrderForItem(UUID orderItemId) {
        OrderQueryRepository.OrderStateRow order = queryRepository.findOrderForItem(orderItemId).orElse(null);
        if (order == null || "draft".equals(order.status())) {
            return;
        }
        refreshOrder(order.orderId());
    }

    @Transactional
    public void applyKitchenLineStatuses(UUID orderId, List<OrderItemLineStatusUpdate> lineStatuses) {
        if (lineStatuses == null || lineStatuses.isEmpty()) {
            refreshOrder(orderId);
            return;
        }
        for (OrderItemLineStatusUpdate update : lineStatuses) {
            ensureKitchenControlledStatus(update.lineStatus());
            repository.updateOrderLineStatus(orderId, update.orderItemId(), update.lineStatus());
        }
        refreshOrder(orderId);
    }

    public void refreshOrder(UUID orderId) {
        List<String> statuses = queryRepository.loadOrderItemStatuses(orderId);
        String nextStatus = orderStatusPolicy.deriveOrderStatus(statuses);
        repository.updateOrderStatus(orderId, nextStatus);
    }

    private void ensureKitchenControlledStatus(String status) {
        if (!KITCHEN_CONTROLLED_STATUSES.contains(status)) {
            throw new ConflictException("Kitchen cannot apply order item status " + status + ".");
        }
    }
}
