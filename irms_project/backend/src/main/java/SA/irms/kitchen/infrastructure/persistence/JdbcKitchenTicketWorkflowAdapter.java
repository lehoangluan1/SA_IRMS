package SA.irms.kitchen.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.kitchen.application.port.out.KitchenTicketWorkflowCommandPort;

@Repository
public class JdbcKitchenTicketWorkflowAdapter implements KitchenTicketWorkflowCommandPort {
    private final JdbcClient jdbcClient;

    public JdbcKitchenTicketWorkflowAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void updateTicketState(UUID ticketId, String persistentStatus, Instant deadline) {
        jdbcClient.sql("""
                        update kitchen_tickets
                        set status = :status,
                            target_service_at = :deadline,
                            updated_at = now()
                        where ticket_id = :ticketId
                        """)
                .param("status", persistentStatus)
                .param("deadline", java.sql.Timestamp.from(deadline))
                .param("ticketId", ticketId)
                .update();
    }

    @Override
    public void cancelOpenTicketsForOrder(UUID orderId, String reason) {
        jdbcClient.sql("""
                        update kitchen_ticket_items kti
                        set status = 'blocked',
                            hold_reason = :reason,
                            updated_at = now()
                        from kitchen_tickets kt
                        where kt.ticket_id = kti.ticket_id
                          and kt.order_id = :orderId
                          and kti.status not in ('served', 'blocked')
                        """)
                .param("orderId", orderId)
                .param("reason", reason)
                .update();
        jdbcClient.sql("""
                        update kitchen_tickets
                        set status = 'blocked', updated_at = now()
                        where order_id = :orderId
                          and status not in ('served', 'blocked')
                        """)
                .param("orderId", orderId)
                .update();
    }
}
