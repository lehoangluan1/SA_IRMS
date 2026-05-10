package SA.irms.kitchen.infrastructure.persistence;

import SA.irms.kitchen.application.port.out.KitchenTicketStateCoordinatorPort;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import SA.irms.common.config.AppProperties;
import SA.irms.common.error.NotFoundException;
import SA.irms.kitchen.application.port.out.OrderingStatusSyncPort;
import SA.irms.kitchen.domain.KitchenStatusPolicy;
import SA.irms.kitchen.application.port.out.OrderItemLineStatusUpdate;

@Service
public class JdbcKitchenTicketStateCoordinator implements KitchenTicketStateCoordinatorPort {
    private static final Logger log = LoggerFactory.getLogger(JdbcKitchenTicketStateCoordinator.class);

    private final JdbcClient jdbcClient;
    private final KitchenStatusPolicy kitchenStatusPolicy;
    private final OrderingStatusSyncPort orderStateCoordinator;
    private final AppProperties appProperties;
    private final Clock clock;

    public JdbcKitchenTicketStateCoordinator(JdbcClient jdbcClient, KitchenStatusPolicy kitchenStatusPolicy,
            OrderingStatusSyncPort orderStateCoordinator, AppProperties appProperties, Clock clock) {
        this.jdbcClient = jdbcClient;
        this.kitchenStatusPolicy = kitchenStatusPolicy;
        this.orderStateCoordinator = orderStateCoordinator;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    public void reconcileTicket(UUID ticketId) {
        refreshTicketStatus(ticketId);
        syncOrderItemStatusesAfterCommit(ticketId);
    }

    public void reconcileTicketsForOrderItem(UUID orderItemId) {
        findTicketIdsForOrderItem(orderItemId).forEach(this::refreshTicketStatus);
    }

    public void refreshOrderForTicket(UUID ticketId) {
        UUID orderId = jdbcClient.sql("""
                        select order_id
                        from kitchen_tickets
                        where ticket_id = :ticketId
                        """)
                .param("ticketId", ticketId)
                .query(UUID.class)
                .single();
        orderStateCoordinator.refreshOrder(orderId);
    }

    public void refreshTicketStatus(UUID ticketId) {
        TicketStateRow ticketState = loadTicketState(ticketId);
        List<String> statuses = jdbcClient.sql("""
                        select status
                        from kitchen_ticket_items
                        where ticket_id = :ticketId
                        """)
                .param("ticketId", ticketId)
                .query(String.class)
                .list();
        String nextStatus = kitchenStatusPolicy.deriveTicketStatus(statuses);
        Timestamp nextActionAt = null;
        if ("ready".equals(nextStatus)) {
            nextActionAt = ticketState.nextActionAt() == null || !"ready".equals(ticketState.status())
                    ? Timestamp.from(Instant.now(clock).plusSeconds(randomReadyToServedSeconds()))
                    : ticketState.nextActionAt();
        }
        jdbcClient.sql("""
                        update kitchen_tickets
                        set status = :status,
                            started_at = case
                                when :status = 'cooking' and started_at is null then now()
                                when :status = 'queued' then null
                                else started_at
                            end,
                            ready_at = case
                                when :status = 'ready' and status <> 'ready' then now()
                                when :status in ('queued', 'cooking', 'blocked', 'hold_for_service') then null
                                else ready_at
                            end,
                            next_action_at = :nextActionAt,
                            updated_at = now()
                        where ticket_id = :ticketId
                        """)
                .param("status", nextStatus)
                .param("nextActionAt", nextActionAt)
                .param("ticketId", ticketId)
                .update();
    }

    private void syncOrderItemStatuses(UUID ticketId) {
        TicketOrderStatusProjection projection = loadTicketOrderStatusProjection(ticketId);
        orderStateCoordinator.applyKitchenLineStatuses(projection.orderId(), projection.lineStatuses());
    }

    private void syncOrderItemStatusesAfterCommit(UUID ticketId) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            trySyncOrderItemStatuses(ticketId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                trySyncOrderItemStatuses(ticketId);
            }
        });
    }

    private void trySyncOrderItemStatuses(UUID ticketId) {
        try {
            syncOrderItemStatuses(ticketId);
        } catch (RuntimeException exception) {
            log.warn("Kitchen ticket {} status changed, but ordering line status sync failed.", ticketId, exception);
        }
    }

    private TicketOrderStatusProjection loadTicketOrderStatusProjection(UUID ticketId) {
        UUID orderId = jdbcClient.sql("""
                        select order_id
                        from kitchen_tickets
                        where ticket_id = :ticketId
                        """)
                .param("ticketId", ticketId)
                .query(UUID.class)
                .single();
        List<OrderItemLineStatusUpdate> lineStatuses = jdbcClient.sql("""
                        select order_item_id,
                               case status
                                   when 'queued' then 'sent_to_kitchen'
                                   when 'cooking' then 'cooking'
                                   when 'ready' then 'ready'
                                   when 'blocked' then 'blocked'
                                   when 'hold_for_service' then 'hold_for_service'
                                   when 'served' then 'served'
                                   else status
                               end as line_status
                        from kitchen_ticket_items
                        where ticket_id = :ticketId
                        """)
                .param("ticketId", ticketId)
                .query((rs, rowNum) -> new OrderItemLineStatusUpdate(
                        rs.getObject("order_item_id", UUID.class),
                        rs.getString("line_status")
                ))
                .list();
        return new TicketOrderStatusProjection(orderId, lineStatuses);
    }

    private List<UUID> findTicketIdsForOrderItem(UUID orderItemId) {
        return jdbcClient.sql("""
                        select distinct ticket_id
                        from kitchen_ticket_items
                        where order_item_id = :orderItemId
                        """)
                .param("orderItemId", orderItemId)
                .query(UUID.class)
                .list();
    }

    private TicketStateRow loadTicketState(UUID ticketId) {
        return jdbcClient.sql("""
                        select status, next_action_at
                        from kitchen_tickets
                        where ticket_id = :ticketId
                        """)
                .param("ticketId", ticketId)
                .query((rs, rowNum) -> new TicketStateRow(
                        kitchenStatusPolicy.normalize(rs.getString("status")),
                        rs.getTimestamp("next_action_at")
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Kitchen ticket was not found."));
    }

    private int randomReadyToServedSeconds() {
        int baseSeconds = Math.max(45, appProperties.kitchenAutomation().readyToServedSeconds());
        int jitter = Math.min(20, Math.max(10, baseSeconds / 6));
        return ThreadLocalRandom.current().nextInt(Math.max(30, baseSeconds - jitter), baseSeconds + jitter + 1);
    }

    private record TicketOrderStatusProjection(UUID orderId, List<OrderItemLineStatusUpdate> lineStatuses) {
    }

    private record TicketStateRow(String status, Timestamp nextActionAt) {
    }
}
