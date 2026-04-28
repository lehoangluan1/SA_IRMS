package SA.irms.kitchen.infrastructure.persistence;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import SA.irms.kitchen.application.KitchenOrderItemCommand;
import SA.irms.kitchen.application.port.out.KitchenOrderRoutingRepository;

@Repository
public class JdbcKitchenOrderRoutingRepository implements KitchenOrderRoutingRepository {
    private final JdbcKitchenRoutePlanRepository routePlanRepository;
    private final JdbcKitchenTicketRoutingRepository ticketRoutingRepository;
    private final JdbcKitchenItemStateRepository itemStateRepository;

    public JdbcKitchenOrderRoutingRepository(
            JdbcKitchenRoutePlanRepository routePlanRepository,
            JdbcKitchenTicketRoutingRepository ticketRoutingRepository,
            JdbcKitchenItemStateRepository itemStateRepository
    ) {
        this.routePlanRepository = routePlanRepository;
        this.ticketRoutingRepository = ticketRoutingRepository;
        this.itemStateRepository = itemStateRepository;
    }

    @Override
    public long countTicketItems(UUID orderItemId) {
        return ticketRoutingRepository.countTicketItems(orderItemId);
    }

    @Override
    public UUID createRoutePlan(UUID orderId, String notes) {
        return routePlanRepository.createRoutePlan(orderId, notes);
    }

    @Override
    public UUID resolveStationId(String stationKind) {
        return routePlanRepository.resolveStationId(stationKind);
    }

    @Override
    public UUID createTicket(UUID orderId, UUID routePlanId, UUID stationId) {
        return ticketRoutingRepository.createTicket(orderId, routePlanId, stationId);
    }

    @Override
    public int createTicketItem(UUID ticketId, KitchenOrderItemCommand item) {
        return ticketRoutingRepository.createTicketItem(ticketId, item);
    }

    @Override
    public void deleteEmptyTicket(UUID ticketId) {
        ticketRoutingRepository.deleteEmptyTicket(ticketId);
    }

    @Override
    public void deleteEmptyRoutePlan(UUID routePlanId) {
        routePlanRepository.deleteEmptyRoutePlan(routePlanId);
    }

    @Override
    public void holdOrderItemForService(UUID orderItemId) {
        itemStateRepository.holdOrderItemForService(orderItemId);
    }

    @Override
    public void releaseHeldOrderItem(UUID orderItemId) {
        itemStateRepository.releaseHeldOrderItem(orderItemId);
    }

    @Override
    public void blockOrderItem(UUID orderItemId, String reason) {
        itemStateRepository.blockOrderItem(orderItemId, reason);
    }
}
