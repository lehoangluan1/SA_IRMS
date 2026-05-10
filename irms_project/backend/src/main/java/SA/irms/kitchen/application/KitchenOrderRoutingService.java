package SA.irms.kitchen.application;

import SA.irms.kitchen.application.port.out.KitchenOrderRoutingRepository;
import SA.irms.kitchen.application.port.out.KitchenTicketStateCoordinatorPort;
import SA.irms.kitchen.application.events.KitchenTicketCreatedEvent;
import SA.irms.common.outbox.DomainEventPublisher;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KitchenOrderRoutingService {
    private final KitchenOrderRoutingRepository routingRepository;
    private final KitchenTicketStateCoordinatorPort ticketStateCoordinator;
    private final DomainEventPublisher outboxPublisher;

    public KitchenOrderRoutingService(
            KitchenOrderRoutingRepository routingRepository,
            KitchenTicketStateCoordinatorPort ticketStateCoordinator,
            DomainEventPublisher outboxPublisher
    ) {
        this.routingRepository = routingRepository;
        this.ticketStateCoordinator = ticketStateCoordinator;
        this.outboxPublisher = outboxPublisher;
    }

    @Transactional
    public void queueConfirmedItems(UUID orderId, List<KitchenOrderItemCommand> items, String notes) {
        List<KitchenOrderItemCommand> routableItems = routableItems(items);
        if (routableItems.isEmpty()) {
            return;
        }
        UUID routePlanId = routingRepository.createRoutePlan(orderId, notes);
        int insertedItems = 0;
        Map<String, List<KitchenOrderItemCommand>> byStation = routableItems.stream()
                .collect(Collectors.groupingBy(KitchenOrderItemCommand::station));
        for (Map.Entry<String, List<KitchenOrderItemCommand>> stationGroup : byStation.entrySet()) {
            UUID stationId = routingRepository.resolveStationId(stationGroup.getKey());
            UUID ticketId = routingRepository.createTicket(orderId, routePlanId, stationId);
            int insertedForTicket = 0;
            for (KitchenOrderItemCommand item : stationGroup.getValue()) {
                insertedForTicket += routingRepository.createTicketItem(ticketId, item);
            }
            insertedItems += insertedForTicket;
            if (insertedForTicket == 0) {
                routingRepository.deleteEmptyTicket(ticketId);
            } else {
                outboxPublisher.publish(new KitchenTicketCreatedEvent(ticketId.toString(), Map.of(
                        "ticketId", ticketId.toString(),
                        "orderId", orderId.toString(),
                        "station", stationGroup.getKey(),
                        "itemCount", insertedForTicket
                )), "kitchen-route-" + orderId, null);
            }
        }
        if (insertedItems == 0) {
            routingRepository.deleteEmptyRoutePlan(routePlanId);
        }
    }

    @Transactional
    public void holdOrderItemForService(UUID orderItemId) {
        routingRepository.holdOrderItemForService(orderItemId);
        ticketStateCoordinator.reconcileTicketsForOrderItem(orderItemId);
    }

    @Transactional
    public void releaseHeldOrderItem(UUID orderId, KitchenOrderItemCommand item, String notes) {
        if (routingRepository.countTicketItems(item.orderItemId()) == 0) {
            queueConfirmedItems(orderId, List.of(item), notes);
            return;
        }
        routingRepository.releaseHeldOrderItem(item.orderItemId());
        ticketStateCoordinator.reconcileTicketsForOrderItem(item.orderItemId());
    }

    @Transactional
    public void blockOrderItem(UUID orderItemId, String reason) {
        routingRepository.blockOrderItem(orderItemId, reason);
        ticketStateCoordinator.reconcileTicketsForOrderItem(orderItemId);
    }

    private List<KitchenOrderItemCommand> routableItems(List<KitchenOrderItemCommand> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .filter(item -> routingRepository.countTicketItems(item.orderItemId()) == 0)
                .toList();
    }
}
