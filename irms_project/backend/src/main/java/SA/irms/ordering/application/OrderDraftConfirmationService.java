package SA.irms.ordering.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import SA.irms.common.audit.AuditRecorder;
import SA.irms.common.error.ConflictException;
import SA.irms.common.security.AuthenticatedUser;
import SA.irms.ordering.application.port.out.OrderRepository;
import SA.irms.common.context.RequestMetadata;
import SA.irms.ordering.application.events.OrderConfirmedEvent;
import SA.irms.common.outbox.DomainEventPublisher;
import SA.irms.ordering.application.KitchenCoordinationPort.KitchenOrderItemCommand;

@Service
class OrderDraftConfirmationService {
    private final AuditRecorder auditService;
    private final DomainEventPublisher outboxPublisher;
    private final OrderMenuSelectionService orderMenuSelectionService;
    private final OrderSnapshotService orderSnapshotService;
    private final OrderDraftRepository orderDraftRepository;
    private final OrdersReadService ordersReadService;
    private final OrderRepository repository;
    private final Clock clock;

    OrderDraftConfirmationService(
            OrderRepository repository,
            AuditRecorder auditService,
            DomainEventPublisher outboxPublisher,
            OrderMenuSelectionService orderMenuSelectionService,
            OrderSnapshotService orderSnapshotService,
            OrderDraftRepository orderDraftRepository,
            OrdersReadService ordersReadService,
            Clock clock
    ) {
        this.repository = repository;
        this.auditService = auditService;
        this.outboxPublisher = outboxPublisher;
        this.orderMenuSelectionService = orderMenuSelectionService;
        this.orderSnapshotService = orderSnapshotService;
        this.orderDraftRepository = orderDraftRepository;
        this.ordersReadService = ordersReadService;
        this.clock = clock;
    }

    @Transactional
    SA.irms.ordering.application.view.OrderViews.OrdersOverview confirmDraftOrder(UUID orderId, AuthenticatedUser actor, String correlationId, RequestMetadata httpServletRequest) {
        DraftOrderRow draftOrder = orderDraftRepository.loadDraftOrder(orderId);
        if (!"draft".equals(draftOrder.status())) {
            throw new ConflictException("Only draft orders can be confirmed.");
        }
        List<DraftOrderItemRow> orderItems = orderDraftRepository.loadDraftOrderItems(orderId);
        if (orderItems.isEmpty()) {
            throw new ConflictException("Draft order does not contain any items.");
        }
        Instant now = Instant.now(clock);
        ConfirmedDraft confirmed = refreshDraftItems(orderId, orderItems);
        orderSnapshotService.upsertOrderSnapshot(orderId, confirmed.subtotal(), now);
        repository.confirmDraftOrder(orderId, now);
        outboxPublisher.publish(new OrderConfirmedEvent(orderId.toString(), confirmedPayload(orderId, draftOrder, confirmed)), correlationId, null);
        auditService.record(actor.userId(), "order.confirmed", "Order", orderId.toString(), correlationId, null, false,
                httpServletRequest.remoteIp(), Map.of("status", "draft"), Map.of("status", "confirmed", "itemCount", confirmed.kitchenItems().size(), "subtotal", confirmed.subtotal()));
        return ordersReadService.load(draftOrder.tableSessionId());
    }

    private ConfirmedDraft refreshDraftItems(UUID orderId, List<DraftOrderItemRow> orderItems) {
        BigDecimal subtotal = BigDecimal.ZERO;
        List<KitchenOrderItemCommand> kitchenItems = new ArrayList<>();
        for (DraftOrderItemRow row : orderItems) {
            OrderMenuSelectionService.ResolvedMenuItem menuItem = orderMenuSelectionService.requireAvailableMenuItem(row.menuItemId(), true);
            OrderMenuSelectionService.ModifierSelection modifierSelection = orderMenuSelectionService.resolveModifierSelection(menuItem.id(), row.modifierOptionIds());
            repository.updateDraftOrderItem(row.orderItemId(), menuItem.name(), menuItem.basePrice().add(modifierSelection.extraPrice()));
            orderMenuSelectionService.refreshModifierSelections(row.orderItemId(), modifierSelection.selectedOptions());
            repository.updateOrderLineStatus(orderId, row.orderItemId(), "sent_to_kitchen");
            subtotal = subtotal.add(menuItem.basePrice().add(modifierSelection.extraPrice()).multiply(BigDecimal.valueOf(row.quantity())));
            kitchenItems.add(new KitchenOrderItemCommand(row.orderItemId(), menuItem.station(), row.quantity()));
        }
        List<Map<String, Object>> comboPayloads = orderDraftRepository.loadDraftComboSelections(orderId).stream()
                .map(selection -> Map.<String, Object>of(
                        "comboId", selection.comboId().toString(),
                        "comboName", selection.comboName(),
                        "quantity", selection.quantity(),
                        "allergyNotes", selection.allergyNotes() == null ? "" : selection.allergyNotes(),
                        "specialInstructions", selection.specialInstructions() == null ? "" : selection.specialInstructions(),
                        "groups", selection.groups().stream().map(group -> Map.<String, Object>of(
                                "comboGroupId", group.comboGroupId().toString(),
                                "selectedOptionIds", group.selectedOptionIds().stream().map(UUID::toString).toList()
                        )).toList()
                ))
                .toList();
        return new ConfirmedDraft(subtotal, kitchenItems, comboPayloads);
    }

    private Map<String, Object> confirmedPayload(UUID orderId, DraftOrderRow draftOrder, ConfirmedDraft confirmed) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId.toString());
        payload.put("tableSessionId", draftOrder.tableSessionId().toString());
        payload.put("specialInstructions", draftOrder.specialInstructions() == null ? "" : draftOrder.specialInstructions());
        payload.put("items", confirmed.kitchenItems().stream().map(item -> Map.<String, Object>of(
                "orderItemId", item.orderItemId().toString(),
                "station", item.station(),
                "quantity", item.quantity()
        )).toList());
        payload.put("combos", confirmed.comboPayloads());
        payload.put("subtotal", confirmed.subtotal());
        return payload;
    }

    private record ConfirmedDraft(BigDecimal subtotal, List<KitchenOrderItemCommand> kitchenItems, List<Map<String, Object>> comboPayloads) {
    }
}
