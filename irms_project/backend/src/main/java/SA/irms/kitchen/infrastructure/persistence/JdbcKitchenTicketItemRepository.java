package SA.irms.kitchen.infrastructure.persistence;
import SA.irms.kitchen.application.query.KitchenTicketItemContext;

import SA.irms.kitchen.application.port.out.KitchenTicketItemRepositoryPort;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.common.error.ConflictException;
import SA.irms.common.error.NotFoundException;
import SA.irms.kitchen.domain.KitchenStatusPolicy;

@Repository
public class JdbcKitchenTicketItemRepository implements KitchenTicketItemRepositoryPort {
    private final JdbcClient jdbcClient;
    private final KitchenStatusPolicy kitchenStatusPolicy;

    JdbcKitchenTicketItemRepository(JdbcClient jdbcClient, KitchenStatusPolicy kitchenStatusPolicy) {
        this.jdbcClient = jdbcClient;
        this.kitchenStatusPolicy = kitchenStatusPolicy;
    }

    public List<KitchenTicketItemContext> loadTicketItemContexts(UUID ticketId, List<String> statuses) {
        return jdbcClient.sql(contextSelectSql() + " where kti.ticket_id = :ticketId order by kti.created_at")
                .param("ticketId", ticketId)
                .query((rs, rowNum) -> new KitchenTicketItemContext(
                        rs.getObject("ticket_item_id", UUID.class),
                        rs.getObject("ticket_id", UUID.class),
                        rs.getObject("order_item_id", UUID.class),
                        kitchenStatusPolicy.normalize(rs.getString("status")),
                        rs.getObject("server_user_id", UUID.class),
                        rs.getString("snapshot_name"),
                        rs.getTimestamp("served_at") == null ? null : rs.getTimestamp("served_at").toInstant(),
                        rs.getTimestamp("next_action_at") == null ? null : rs.getTimestamp("next_action_at").toInstant(),
                        rs.getTimestamp("cooking_started_at") == null ? null : rs.getTimestamp("cooking_started_at").toInstant(),
                        rs.getTimestamp("inventory_deducted_at") == null ? null : rs.getTimestamp("inventory_deducted_at").toInstant(),
                        rs.getString("table_code"),
                        rs.getString("station_kind")
                ))
                .list()
                .stream()
                .filter(item -> statuses.contains(item.status()))
                .toList();
    }

    public KitchenTicketItemContext loadForUpdate(UUID ticketItemId) {
        return loadForUpdate(ticketItemId, true);
    }

    public KitchenTicketItemContext loadForAutomationUpdate(UUID ticketItemId) {
        return loadForUpdate(ticketItemId, false);
    }

    public boolean everyTicketItemReady(UUID ticketId) {
        long nonReadyCount = jdbcClient.sql("""
                        select count(*)
                        from kitchen_ticket_items
                        where ticket_id = :ticketId
                          and status not in ('ready', 'blocked')
                        """)
                .param("ticketId", ticketId)
                .query(Long.class)
                .single();
        return nonReadyCount == 0;
    }

    public long countItemsNotReadyForServing(UUID ticketId) {
        return jdbcClient.sql("""
                        select count(*)
                        from kitchen_ticket_items
                        where ticket_id = :ticketId
                          and status not in ('ready', 'served', 'blocked')
                        """)
                .param("ticketId", ticketId)
                .query(Long.class)
                .single();
    }

    private KitchenTicketItemContext loadForUpdate(UUID ticketItemId, boolean notFoundException) {
        return jdbcClient.sql(contextSelectSql() + " where kti.ticket_item_id = :ticketItemId for update")
                .param("ticketItemId", ticketItemId)
                .query((rs, rowNum) -> new KitchenTicketItemContext(
                        rs.getObject("ticket_item_id", UUID.class),
                        rs.getObject("ticket_id", UUID.class),
                        rs.getObject("order_item_id", UUID.class),
                        kitchenStatusPolicy.normalize(rs.getString("status")),
                        rs.getObject("server_user_id", UUID.class),
                        rs.getString("snapshot_name"),
                        rs.getTimestamp("served_at") == null ? null : rs.getTimestamp("served_at").toInstant(),
                        rs.getTimestamp("next_action_at") == null ? null : rs.getTimestamp("next_action_at").toInstant(),
                        rs.getTimestamp("cooking_started_at") == null ? null : rs.getTimestamp("cooking_started_at").toInstant(),
                        rs.getTimestamp("inventory_deducted_at") == null ? null : rs.getTimestamp("inventory_deducted_at").toInstant(),
                        rs.getString("table_code"),
                        rs.getString("station_kind")
                ))
                .optional()
                .orElseThrow(() -> notFoundException
                        ? new NotFoundException("Kitchen item was not found.")
                        : new ConflictException("Kitchen item context was not found."));
    }

    private String contextSelectSql() {
        return """
                        select kti.ticket_item_id,
                               kti.ticket_id,
                               kti.order_item_id,
                               kti.status,
                               o.server_user_id,
                               oi.snapshot_name,
                               oi.served_at,
                               kti.next_action_at,
                               kti.cooking_started_at,
                               kti.inventory_deducted_at,
                               dt.code as table_code,
                               ks.kind as station_kind
                        from kitchen_ticket_items kti
                        join kitchen_tickets kt on kt.ticket_id = kti.ticket_id
                        join kitchen_stations ks on ks.station_id = kt.station_id
                        join orders o on o.order_id = kt.order_id
                        join table_sessions ts on ts.session_id = o.table_session_id
                        join dining_tables dt on dt.table_id = ts.table_id
                        join order_items oi on oi.order_item_id = kti.order_item_id
                       """;
    }
}