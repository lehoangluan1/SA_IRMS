package SA.irms.ordering.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.ordering.application.port.out.OrderQueryRepository;

@Component
class OrderDraftRepository {
    private final OrderQueryRepository queryRepository;

    OrderDraftRepository(OrderQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    DraftOrderRow loadDraftOrder(UUID orderId) {
        OrderQueryRepository.DraftOrderRow row = queryRepository.loadDraftOrder(orderId);
        return new DraftOrderRow(row.orderId(), row.tableSessionId(), row.status(), row.specialInstructions());
    }

    List<DraftOrderItemRow> loadDraftOrderItems(UUID orderId) {
        return queryRepository.loadDraftOrderItems(orderId).stream()
                .map(row -> new DraftOrderItemRow(row.orderItemId(), row.menuItemId(), row.quantity(), row.status(), row.modifierOptionIds()))
                .toList();
    }

    java.util.List<OrderQueryRepository.OrderComboSelectionPayloadRow> loadDraftComboSelections(UUID orderId) {
        return queryRepository.loadOrderComboSelections(orderId);
    }
}

