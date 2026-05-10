package SA.irms.ordering.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import SA.irms.ordering.application.port.out.OrderQueryRepository;

final class JdbcOrderRoutingStatusQueryRepository {
    private final JdbcClient jdbcClient;

    JdbcOrderRoutingStatusQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    Optional<OrderQueryRepository.OrderItemRoutingRow> findOrderItemRouting(UUID orderItemId) {
        return jdbcClient.sql("""
                        select oi.order_id,
                               o.status as order_status,
                               oi.quantity,
                               oi.line_status,
                               m.station
                        from order_items oi
                        join orders o on o.order_id = oi.order_id
                        join menu_items m on m.menu_item_id = oi.menu_item_id
                        where oi.order_item_id = :orderItemId
                        """)
                .param("orderItemId", orderItemId)
                .query((rs, rowNum) -> new OrderQueryRepository.OrderItemRoutingRow(
                        rs.getObject("order_id", UUID.class),
                        rs.getString("order_status"),
                        rs.getInt("quantity"),
                        rs.getString("line_status"),
                        rs.getString("station")
                ))
                .optional();
    }
}
