package SA.irms.ordering.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.common.error.NotFoundException;
import SA.irms.ordering.application.port.out.OrderQueryRepository;

final class JdbcDraftOrderQueryRepository {
    private final JdbcClient jdbcClient;
    private final JdbcModifierQueryRepository modifierQueryRepository;

    JdbcDraftOrderQueryRepository(JdbcClient jdbcClient, JdbcModifierQueryRepository modifierQueryRepository) {
        this.jdbcClient = jdbcClient;
        this.modifierQueryRepository = modifierQueryRepository;
    }

    OrderQueryRepository.DraftOrderRow loadDraftOrder(UUID orderId) {
        return jdbcClient.sql("""
                        select order_id, table_session_id, status, special_instructions
                        from orders
                        where order_id = :orderId
                        """)
                .param("orderId", orderId)
                .query((rs, rowNum) -> new OrderQueryRepository.DraftOrderRow(
                        rs.getObject("order_id", UUID.class),
                        rs.getObject("table_session_id", UUID.class),
                        rs.getString("status"),
                        rs.getString("special_instructions")
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Order was not found."));
    }

    List<OrderQueryRepository.DraftOrderItemRow> loadDraftOrderItems(UUID orderId) {
        return jdbcClient.sql("""
                        select oi.order_item_id, oi.menu_item_id, oi.quantity, oi.line_status
                        from order_items oi
                        where oi.order_id = :orderId
                        order by oi.created_at
                        """)
                .param("orderId", orderId)
                .query((rs, rowNum) -> new OrderQueryRepository.DraftOrderItemRow(
                        rs.getObject("order_item_id", UUID.class),
                        rs.getObject("menu_item_id", UUID.class),
                        rs.getInt("quantity"),
                        rs.getString("line_status"),
                        modifierQueryRepository.loadModifierOptionIds(rs.getObject("order_item_id", UUID.class))
                ))
                .list();
    }
}
