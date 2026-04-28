package SA.irms.ordering.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.common.error.ConflictException;
import SA.irms.common.error.NotFoundException;
import SA.irms.ordering.application.command.OrderCommands.ComboSelectionRequest;
import SA.irms.ordering.application.command.OrderCommands.CreateOrderRequest;
import SA.irms.ordering.application.command.OrderCommands.OrderItemRequest;
import SA.irms.ordering.application.port.out.OrderQueryRepository;
import SA.irms.ordering.application.port.out.OrderRepository;

@Component
class OrderCreationValidator {
    private final OrderRepository repository;

    OrderCreationValidator(OrderRepository repository) {
        this.repository = repository;
    }

    void validateCreateOrderRequest(CreateOrderRequest request) {
        ensureSessionAcceptsOrders(request.sessionId());
        if ((request.items() == null || request.items().isEmpty())
                && (request.comboSelections() == null || request.comboSelections().isEmpty())) {
            throw new ConflictException("At least one order item or combo selection is required.");
        }
    }

    void validateOrderItem(OrderItemRequest itemRequest) {
        if (itemRequest.quantity() < 1) {
            throw new ConflictException("Order item quantity must be at least 1.");
        }
    }

    void validateComboQuantity(ComboSelectionRequest comboSelection) {
        if (comboSelection.quantity() < 1) {
            throw new ConflictException("Combo quantity must be at least 1.");
        }
    }

    void validateComboGroupsPresent(ComboSelectionRequest comboSelection) {
        if (comboSelection.groups() == null || comboSelection.groups().isEmpty()) {
            throw new ConflictException("Combo selections must include at least one group.");
        }
    }

    void validateGroupSelection(OrderQueryRepository.ResolvedComboGroupRow group, List<UUID> selectedOptionIds) {
        if (group.required() && selectedOptionIds.size() < group.minSelections()) {
            throw new ConflictException("Required combo selections are missing for group " + group.name() + ".");
        }
        if (selectedOptionIds.size() > group.maxSelections()) {
            throw new ConflictException("Too many combo selections were chosen for group " + group.name() + ".");
        }
    }

    void ensureComboIsActive(OrderQueryRepository.ResolvedComboRow combo) {
        if (!combo.active()) {
            throw new ConflictException("Selected combo is not active.");
        }
    }

    void ensureResolvedComboOptions(OrderQueryRepository.ResolvedComboGroupRow group, List<UUID> selectedOptionIds,
                                    List<OrderQueryRepository.ResolvedComboOptionRow> options) {
        if (options.size() != selectedOptionIds.size()) {
            throw new ConflictException("One or more combo options are invalid for group " + group.name() + ".");
        }
    }

    private void ensureSessionAcceptsOrders(UUID sessionId) {
        String sessionStatus = repository.findSessionStatus(sessionId)
                .orElseThrow(() -> new NotFoundException("Table session was not found."));
        if (!"active".equals(sessionStatus) && !"billing".equals(sessionStatus)) {
            throw new ConflictException("Orders can only be created for active table sessions.");
        }
    }
}
