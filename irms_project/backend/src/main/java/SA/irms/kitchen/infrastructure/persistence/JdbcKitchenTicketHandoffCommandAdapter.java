package SA.irms.kitchen.infrastructure.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.kitchen.application.port.out.KitchenTicketHandoffCommandPort;

@Repository
public class JdbcKitchenTicketHandoffCommandAdapter implements KitchenTicketHandoffCommandPort {
    private final JdbcClient jdbcClient;

    public JdbcKitchenTicketHandoffCommandAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<UUID> findHandoffId(UUID ticketId) {
        return jdbcClient.sql("select handoff_id from serve_handoffs where ticket_id = :ticketId")
                .param("ticketId", ticketId)
                .query(UUID.class)
                .optional();
    }

    @Override
    public UUID createHandoff(UUID ticketId, UUID serverId) {
        UUID newHandoffId = UUID.randomUUID();
        jdbcClient.sql("""
                        insert into serve_handoffs (handoff_id, ticket_id, server_id, handed_off_at, accepted_at, delivered_at)
                        values (:handoffId, :ticketId, :serverId, now(), now(), now())
                        """)
                .param("handoffId", newHandoffId)
                .param("ticketId", ticketId)
                .param("serverId", serverId)
                .update();
        return newHandoffId;
    }

    @Override
    public void markReadyItemsServed(UUID ticketId) {
        jdbcClient.sql("""
                        update kitchen_ticket_items
                        set status = 'served',
                            next_action_at = null,
                            updated_at = now()
                        where ticket_id = :ticketId
                          and status = 'ready'
                        """)
                .param("ticketId", ticketId)
                .update();
    }

    @Override
    public void markTicketServed(UUID ticketId) {
        jdbcClient.sql("""
                        update kitchen_tickets
                        set status = 'served',
                            served_at = now(),
                            completed_at = now(),
                            next_action_at = null,
                            updated_at = now()
                        where ticket_id = :ticketId
                        """)
                .param("ticketId", ticketId)
                .update();
    }

    @Override
    public void markHandoffReturned(UUID handoffId, String reason) {
        jdbcClient.sql("""
                        update serve_handoffs
                        set returned_at = now(),
                            return_reason = :reason
                        where handoff_id = :handoffId
                        """)
                .param("reason", reason)
                .param("handoffId", handoffId)
                .update();
    }

    @Override
    public void markServedItemsReady(UUID ticketId, String reason) {
        jdbcClient.sql("""
                        update kitchen_ticket_items
                        set status = 'ready',
                            hold_reason = :reason,
                            updated_at = now()
                        where ticket_id = :ticketId
                          and status = 'served'
                        """)
                .param("reason", reason)
                .param("ticketId", ticketId)
                .update();
    }

    @Override
    public void markTicketReturned(UUID ticketId, Instant nextActionAt) {
        jdbcClient.sql("""
                        update kitchen_tickets
                        set status = 'ready',
                            served_at = null,
                            completed_at = null,
                            ready_at = coalesce(ready_at, now()),
                            next_action_at = :nextActionAt,
                            updated_at = now()
                        where ticket_id = :ticketId
                        """)
                .param("nextActionAt", Timestamp.from(nextActionAt))
                .param("ticketId", ticketId)
                .update();
    }
}
