package SA.irms.ordering.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

final class JdbcOrderLifecycleRepository {
    private final JdbcClient jdbcClient;

    JdbcOrderLifecycleRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    Optional<String> findSessionStatus(UUID sessionId) {
        return jdbcClient.sql("select status from table_sessions where session_id = :sessionId")
                .param("sessionId", sessionId)
                .query(String.class)
                .optional();
    }

    void createOrder(UUID orderId, UUID sessionId, UUID serverUserId, String status, String specialInstructions,
                     String correlationId, Instant createdAt, Instant confirmedAt) {
        jdbcClient.sql("""
                        insert into orders (order_id, table_session_id, channel, server_user_id, status, special_instructions,
                                            correlation_id, created_at, confirmed_at)
                        values (:orderId, :sessionId, 'dine_in', :serverUserId, :status, :specialInstructions,
                                :correlationId, :createdAt, :confirmedAt)
                        """)
                .param("orderId", orderId)
                .param("sessionId", sessionId)
                .param("serverUserId", serverUserId)
                .param("status", status)
                .param("specialInstructions", specialInstructions)
                .param("correlationId", correlationId)
                .param("createdAt", toTimestamp(createdAt))
                .param("confirmedAt", toTimestamp(confirmedAt))
                .update();
    }

    void updateOrderSnapshot(UUID orderId, BigDecimal subtotal, Instant pricedAt) {
        long snapshotCount = jdbcClient.sql("select count(*) from order_snapshots where order_id = :orderId")
                .param("orderId", orderId)
                .query(Long.class)
                .single();
        if (snapshotCount == 0) {
            jdbcClient.sql("""
                            insert into order_snapshots (snapshot_id, order_id, menu_version, priced_at, currency, subtotal)
                            values (:snapshotId, :orderId, 'menu-v1', :pricedAt, 'USD', :subtotal)
                            """)
                    .param("snapshotId", UUID.randomUUID())
                    .param("orderId", orderId)
                    .param("pricedAt", toTimestamp(pricedAt))
                    .param("subtotal", subtotal)
                    .update();
            return;
        }
        jdbcClient.sql("""
                        update order_snapshots
                        set priced_at = :pricedAt, subtotal = :subtotal
                        where order_id = :orderId
                        """)
                .param("pricedAt", toTimestamp(pricedAt))
                .param("subtotal", subtotal)
                .param("orderId", orderId)
                .update();
    }

    void confirmDraftOrder(UUID orderId, Instant confirmedAt) {
        Timestamp timestamp = toTimestamp(confirmedAt);
        jdbcClient.sql("""
                        update order_items
                        set line_status = 'sent_to_kitchen', sent_to_kitchen_at = coalesce(sent_to_kitchen_at, :confirmedAt),
                            fire_at = :confirmedAt, updated_at = now()
                        where order_id = :orderId and line_status = 'pending'
                        """)
                .param("confirmedAt", timestamp)
                .param("orderId", orderId)
                .update();
        jdbcClient.sql("""
                        update orders
                        set status = 'confirmed', confirmed_at = :confirmedAt, updated_at = now()
                        where order_id = :orderId
                        """)
                .param("confirmedAt", timestamp)
                .param("orderId", orderId)
                .update();
    }

    void updateOrderStatus(UUID orderId, String status) {
        jdbcClient.sql("""
                        update orders
                        set status = :status,
                            updated_at = now()
                        where order_id = :orderId
                          and status <> 'draft'
                        """)
                .param("status", status)
                .param("orderId", orderId)
                .update();
    }

    void markOrderCancelled(UUID orderId) {
        jdbcClient.sql("""
                        update orders
                        set status = 'cancelled', updated_at = now()
                        where order_id = :orderId
                        """)
                .param("orderId", orderId)
                .update();
    }

    Optional<String> findOrderStatus(UUID orderId) {
        return jdbcClient.sql("select status from orders where order_id = :orderId")
                .param("orderId", orderId)
                .query(String.class)
                .optional();
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
