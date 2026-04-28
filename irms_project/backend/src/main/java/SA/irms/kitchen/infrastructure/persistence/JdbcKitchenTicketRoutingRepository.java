package SA.irms.kitchen.infrastructure.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.kitchen.application.KitchenOrderItemCommand;

@Repository
class JdbcKitchenTicketRoutingRepository {
    private final JdbcClient jdbcClient;

    JdbcKitchenTicketRoutingRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    long countTicketItems(UUID orderItemId) {
        return jdbcClient.sql("""
                        select count(*)
                        from kitchen_ticket_items
                        where order_item_id = :orderItemId
                        """)
                .param("orderItemId", orderItemId)
                .query(Long.class)
                .single();
    }

    UUID createTicket(UUID orderId, UUID routePlanId, UUID stationId) {
        UUID ticketId = UUID.randomUUID();
        jdbcClient.sql("""
                        insert into kitchen_tickets (
                            ticket_id,
                            order_id,
                            station_id,
                            route_plan_id,
                            priority,
                            priority_label,
                            status,
                            created_at,
                            target_service_at,
                            next_action_at
                        ) values (
                            :ticketId,
                            :orderId,
                            :stationId,
                            :routePlanId,
                            5,
                            'normal',
                            'queued',
                            now(),
                            now() + interval '15 minutes',
                            null
                        )
                        """)
                .param("ticketId", ticketId)
                .param("orderId", orderId)
                .param("stationId", stationId)
                .param("routePlanId", routePlanId)
                .update();
        return ticketId;
    }

    int createTicketItem(UUID ticketId, KitchenOrderItemCommand item) {
        return jdbcClient.sql("""
                        insert into kitchen_ticket_items (
                            ticket_item_id,
                            ticket_id,
                            order_item_id,
                            quantity,
                            status,
                            fire_at,
                            next_action_at
                        ) values (
                            :ticketItemId,
                            :ticketId,
                            :orderItemId,
                            :quantity,
                            'queued',
                            now(),
                            null
                        )
                        on conflict (order_item_id) do nothing
                        """)
                .param("ticketItemId", UUID.randomUUID())
                .param("ticketId", ticketId)
                .param("orderItemId", item.orderItemId())
                .param("quantity", item.quantity())
                .update();
    }

    void deleteEmptyTicket(UUID ticketId) {
        jdbcClient.sql("""
                        delete from kitchen_tickets kt
                        where kt.ticket_id = :ticketId
                          and not exists (
                              select 1
                              from kitchen_ticket_items kti
                              where kti.ticket_id = kt.ticket_id
                          )
                        """)
                .param("ticketId", ticketId)
                .update();
    }
}
