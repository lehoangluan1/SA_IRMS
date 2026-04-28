package SA.irms.ordering.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.common.error.NotFoundException;
import SA.irms.ordering.application.port.out.OrderQueryRepository;
import SA.irms.ordering.application.view.OrderViews;

final class JdbcOrderedItemQueryRepository {
    private final JdbcClient jdbcClient;

    JdbcOrderedItemQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    List<OrderViews.OrderedItemView> loadOrderedItems(UUID sessionId) {
        return jdbcClient.sql("""
                        select oi.order_item_id, o.order_id, o.status as order_status, oi.snapshot_name, oi.quantity,
                               oi.unit_price + coalesce((select sum(oim.extra_price * oim.qty_multiplier)
                                   from order_item_modifiers oim where oim.order_item_id = oi.order_item_id), 0) as unit_price,
                               coalesce(oi.special_instruction, oi.allergy_notes, '') as note,
                               oi.line_status, m.station
                        from orders o
                        join order_items oi on oi.order_id = o.order_id
                        join menu_items m on m.menu_item_id = oi.menu_item_id
                        where o.table_session_id = :sessionId
                        order by oi.created_at
                        """)
                .param("sessionId", sessionId)
                .query((rs, rowNum) -> new OrderViews.OrderedItemView(
                        rs.getObject("order_item_id", UUID.class),
                        rs.getObject("order_id", UUID.class),
                        rs.getString("order_status"),
                        rs.getString("snapshot_name"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("unit_price"),
                        rs.getString("note"),
                        rs.getString("line_status"),
                        rs.getString("station")
                ))
                .list();
    }

    OrderViews.OrderedItemView loadOrderedItemView(UUID orderItemId) {
        return jdbcClient.sql("""
                        select oi.order_item_id,
                               oi.order_id,
                               o.status as order_status,
                               oi.snapshot_name,
                               oi.quantity,
                               oi.unit_price + coalesce((
                                   select sum(oim.extra_price * oim.qty_multiplier)
                                   from order_item_modifiers oim
                                   where oim.order_item_id = oi.order_item_id
                               ), 0) as unit_price,
                               coalesce(oi.special_instruction, oi.allergy_notes, '') as note,
                               oi.line_status,
                               m.station
                        from order_items oi
                        join orders o on o.order_id = oi.order_id
                        join menu_items m on m.menu_item_id = oi.menu_item_id
                        where oi.order_item_id = :orderItemId
                        """)
                .param("orderItemId", orderItemId)
                .query((rs, rowNum) -> new OrderViews.OrderedItemView(
                        rs.getObject("order_item_id", UUID.class),
                        rs.getObject("order_id", UUID.class),
                        rs.getString("order_status"),
                        rs.getString("snapshot_name"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("unit_price"),
                        rs.getString("note"),
                        rs.getString("line_status"),
                        rs.getString("station")
                ))
                .optional()
                .orElseThrow(() -> new NotFoundException("Order item was not found."));
    }

    Optional<OrderQueryRepository.OrderStateRow> findOrderForItem(UUID orderItemId) {
        return jdbcClient.sql("""
                        select oi.order_id, o.status
                        from order_items oi
                        join orders o on o.order_id = oi.order_id
                        where oi.order_item_id = :orderItemId
                        """)
                .param("orderItemId", orderItemId)
                .query((rs, rowNum) -> new OrderQueryRepository.OrderStateRow(
                        rs.getObject("order_id", UUID.class),
                        rs.getString("status")
                ))
                .optional();
    }

    List<String> loadOrderItemStatuses(UUID orderId) {
        return jdbcClient.sql("""
                        select line_status
                        from order_items
                        where order_id = :orderId
                        """)
                .param("orderId", orderId)
                .query(String.class)
                .list();
    }
}
