package SA.irms.kitchen.infrastructure.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.kitchen.application.port.out.KitchenTicketCancellationCommandPort;

@Repository
public class JdbcKitchenTicketCancellationCommandAdapter implements KitchenTicketCancellationCommandPort {
    private final JdbcClient jdbcClient;

    public JdbcKitchenTicketCancellationCommandAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void blockOpenItems(UUID ticketId, String reason) {
        jdbcClient.sql("""
                        update kitchen_ticket_items
                        set status = 'blocked', hold_reason = :reason, next_action_at = null, updated_at = now()
                        where ticket_id = :ticketId and status not in ('served', 'blocked')
                        """)
                .param("reason", reason)
                .param("ticketId", ticketId)
                .update();
    }

    @Override
    public void blockTicket(UUID ticketId, String reason) {
        jdbcClient.sql("""
                        update kitchen_tickets
                        set status = 'blocked', blocked_reason = :reason, next_action_at = null, updated_at = now()
                        where ticket_id = :ticketId
                        """)
                .param("reason", reason)
                .param("ticketId", ticketId)
                .update();
    }
}
