package SA.irms.kitchen.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.kitchen.application.port.out.KitchenTicketItemCommandPort;

@Repository
public class JdbcKitchenTicketItemCommandAdapter implements KitchenTicketItemCommandPort {
    private final JdbcClient jdbcClient;

    public JdbcKitchenTicketItemCommandAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void moveItemToCooking(UUID ticketItemId, Instant nextActionAt) {
        jdbcClient.sql("""
                        update kitchen_ticket_items
                        set status = 'cooking',
                            hold_reason = null,
                            cooking_started_at = coalesce(cooking_started_at, now()),
                            inventory_deducted_at = coalesce(inventory_deducted_at, now()),
                            next_action_at = :nextActionAt,
                            updated_at = now()
                        where ticket_item_id = :ticketItemId
                          and status = 'queued'
                        """)
                .param("nextActionAt", Timestamp.from(nextActionAt))
                .param("ticketItemId", ticketItemId)
                .update();
    }

    @Override
    public void markItemReady(UUID ticketItemId) {
        jdbcClient.sql("""
                        update kitchen_ticket_items
                        set status = 'ready',
                            hold_reason = null,
                            next_action_at = null,
                            updated_at = now()
                        where ticket_item_id = :ticketItemId
                          and status = 'cooking'
                        """)
                .param("ticketItemId", ticketItemId)
                .update();
    }

    @Override
    public void blockItem(UUID ticketItemId, String reason) {
        jdbcClient.sql("""
                        update kitchen_ticket_items
                        set status = 'blocked',
                            hold_reason = :reason,
                            next_action_at = null,
                            updated_at = now()
                        where ticket_item_id = :ticketItemId
                        """)
                .param("reason", reason)
                .param("ticketItemId", ticketItemId)
                .update();
    }

    @Override
    public void markInventoryDeducted(UUID ticketItemId) {
        jdbcClient.sql("""
                        update kitchen_ticket_items
                        set inventory_deducted_at = now(),
                            cooking_started_at = coalesce(cooking_started_at, now())
                        where ticket_item_id = :ticketItemId
                          and inventory_deducted_at is null
                        """)
                .param("ticketItemId", ticketItemId)
                .update();
    }
}
