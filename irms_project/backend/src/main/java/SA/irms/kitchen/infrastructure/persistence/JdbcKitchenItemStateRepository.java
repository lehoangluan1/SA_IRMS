package SA.irms.kitchen.infrastructure.persistence;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcKitchenItemStateRepository {
    private final JdbcClient jdbcClient;

    JdbcKitchenItemStateRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    void holdOrderItemForService(UUID orderItemId) {
        jdbcClient.sql("""
                        update kitchen_ticket_items
                        set status = 'hold_for_service',
                            fire_at = null,
                            next_action_at = null,
                            updated_at = now()
                        where order_item_id = :orderItemId
                          and status not in ('served', 'blocked')
                        """)
                .param("orderItemId", orderItemId)
                .update();
    }

    void releaseHeldOrderItem(UUID orderItemId) {
        jdbcClient.sql("""
                        update kitchen_ticket_items
                        set status = 'queued',
                            fire_at = now(),
                            next_action_at = null,
                            updated_at = now()
                        where order_item_id = :orderItemId
                          and status = 'hold_for_service'
                        """)
                .param("orderItemId", orderItemId)
                .update();
    }

    void blockOrderItem(UUID orderItemId, String reason) {
        jdbcClient.sql("""
                        update kitchen_ticket_items
                        set status = 'blocked',
                            hold_reason = :reason,
                            next_action_at = null,
                            updated_at = now()
                        where order_item_id = :orderItemId
                        """)
                .param("reason", reason)
                .param("orderItemId", orderItemId)
                .update();
    }
}
