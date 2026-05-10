package SA.irms.ordering.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import SA.irms.ordering.application.command.OrderCommands.ComboSelectionRequest;
import SA.irms.ordering.application.command.OrderCommands.CreateOrderRequest;
import SA.irms.ordering.application.command.OrderCommands.OrderItemRequest;
import SA.irms.ordering.application.port.out.OrderRepository;
import SA.irms.ordering.application.KitchenCoordinationPort.KitchenOrderItemCommand;

@Component
class OrderPersistenceCoordinator {
    private final OrderRepository repository;
    private final OrderMenuSelectionService orderMenuSelectionService;
    private final OrderComboSelectionResolver comboSelectionResolver;
    private final OrderCreationValidator validator;

    OrderPersistenceCoordinator(
            OrderRepository repository,
            OrderMenuSelectionService orderMenuSelectionService,
            OrderComboSelectionResolver comboSelectionResolver,
            OrderCreationValidator validator
    ) {
        this.repository = repository;
        this.orderMenuSelectionService = orderMenuSelectionService;
        this.comboSelectionResolver = comboSelectionResolver;
        this.validator = validator;
    }

    OrderCreationResult createOrder(UUID orderId, CreateOrderRequest request, UUID actorUserId, String correlationId, Instant now) {
        repository.createOrder(
                orderId,
                request.sessionId(),
                actorUserId,
                request.draft() ? "draft" : "confirmed",
                request.specialInstructions(),
                correlationId,
                now,
                request.draft() ? null : now
        );
        return createOrderItems(orderId, request);
    }

    private OrderCreationResult createOrderItems(UUID orderId, CreateOrderRequest request) {
        List<KitchenOrderItemCommand> kitchenItems = new ArrayList<>();
        List<java.util.Map<String, Object>> comboPayloads = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.items() == null ? List.<OrderItemRequest>of() : request.items()) {
            validator.validateOrderItem(itemRequest);
            subtotal = subtotal.add(createStandardItem(orderId, request.draft(), itemRequest, kitchenItems));
        }
        for (ComboSelectionRequest comboSelection : request.comboSelections() == null ? List.<ComboSelectionRequest>of() : request.comboSelections()) {
            subtotal = subtotal.add(createComboSelection(orderId, request.draft(), comboSelection, kitchenItems, comboPayloads));
        }

        return new OrderCreationResult(subtotal, kitchenItems, comboPayloads);
    }

    private BigDecimal createStandardItem(UUID orderId, boolean draft, OrderItemRequest itemRequest,
                                          List<KitchenOrderItemCommand> kitchenItems) {
        OrderMenuSelectionService.ResolvedMenuItem menuItem = orderMenuSelectionService.requireAvailableMenuItem(itemRequest.menuItemId(), false);
        OrderMenuSelectionService.ModifierSelection modifierSelection = orderMenuSelectionService.resolveModifierSelection(
                menuItem.id(),
                itemRequest.modifierOptionIds()
        );
        UUID orderItemId = UUID.randomUUID();
        repository.createOrderItem(
                orderId,
                orderItemId,
                menuItem.id(),
                menuItem.name(),
                itemRequest.quantity(),
                menuItem.basePrice(),
                itemRequest.note(),
                itemRequest.note(),
                lineStatus(draft, itemRequest.sendLater()),
                draft,
                itemRequest.sendLater()
        );
        orderMenuSelectionService.insertModifierSelections(orderItemId, modifierSelection.selectedOptions());
        if (!draft && !itemRequest.sendLater()) {
            kitchenItems.add(new KitchenOrderItemCommand(orderItemId, menuItem.station(), itemRequest.quantity()));
        }
        return menuItem.basePrice()
                .add(modifierSelection.extraPrice())
                .multiply(BigDecimal.valueOf(itemRequest.quantity()));
    }

    private BigDecimal createComboSelection(UUID orderId, boolean draft, ComboSelectionRequest comboSelection,
                                            List<KitchenOrderItemCommand> kitchenItems,
                                            List<java.util.Map<String, Object>> comboPayloads) {
        OrderComboSelectionResolver.ResolvedComboSelection resolved = comboSelectionResolver.resolve(comboSelection);
        UUID comboSelectionId = UUID.randomUUID();
        repository.createOrderComboSelection(
                orderId,
                comboSelectionId,
                resolved.comboId(),
                resolved.comboName(),
                resolved.quantity(),
                resolved.comboPrice(),
                resolved.allergyNotes(),
                resolved.specialInstructions()
        );
        for (OrderComboSelectionResolver.ResolvedComboComponent component : resolved.components()) {
            UUID orderItemId = UUID.randomUUID();
            repository.createOrderItem(
                    orderId,
                    orderItemId,
                    component.menuItemId(),
                    component.menuItemName(),
                    component.quantity(),
                    component.allocatedUnitPrice(),
                    resolved.specialInstructions(),
                    resolved.allergyNotes(),
                    draft ? "pending" : "sent_to_kitchen",
                    draft,
                    false
            );
            repository.createOrderComboSelectionItem(
                    comboSelectionId,
                    component.comboGroupId(),
                    component.comboOptionId(),
                    component.menuItemId(),
                    component.menuItemName(),
                    component.station(),
                    component.quantity(),
                    component.allocatedUnitPrice()
            );
            if (!draft) {
                kitchenItems.add(new KitchenOrderItemCommand(orderItemId, component.station(), component.quantity()));
            }
        }
        comboPayloads.add(resolved.payload());
        return resolved.subtotal();
    }

    private String lineStatus(boolean draft, boolean sendLater) {
        if (sendLater) {
            return "hold_for_service";
        }
        return draft ? "pending" : "sent_to_kitchen";
    }
}
