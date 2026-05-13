package SA.irms.kitchen.application;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import SA.irms.common.error.NotFoundException;
import SA.irms.kitchen.application.view.KitchenViews;
import SA.irms.kitchen.domain.KitchenStatusPolicy;

@Service
class KitchenTicketReadService {
    private static final long RECENT_COMPLETED_TICKET_SECONDS = 60L;

    private final JdbcClient jdbcClient;
    private final KitchenStatusPolicy kitchenStatusPolicy;
    private final Clock clock;

    KitchenTicketReadService(
            JdbcClient jdbcClient,
            KitchenStatusPolicy kitchenStatusPolicy,
            Clock clock
    ) {
        this.jdbcClient = jdbcClient;
        this.kitchenStatusPolicy = kitchenStatusPolicy;
        this.clock = clock;
    }

    public KitchenViews.KitchenOverview load(String stationFilter) {
        List<String> stations = jdbcClient.sql("""
                        select distinct kind
                        from kitchen_stations
                        order by kind
                        """)
                .query(String.class)
                .list();
        return new KitchenViews.KitchenOverview(stations, loadTickets(stationFilter, null, false));
    }

    public KitchenViews.TicketView findTicket(UUID ticketId) {
        return loadTickets(null, ticketId, true).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Kitchen ticket was not found."));
    }

    private List<KitchenViews.TicketView> loadTickets(String stationFilter, UUID ticketId, boolean includeClosedStatuses) {
        String normalizedStationFilter = stationFilter == null || stationFilter.isBlank() || "all".equalsIgnoreCase(stationFilter)
                ? null
                : stationFilter;
        String sql = """
                select kt.ticket_id,
                       o.order_id,
                       dt.code as table_code,
                       u.display_name,
                       kt.priority_label,
                       kt.status as ticket_status,
                       kt.created_at,
                       kti.ticket_item_id,
                       kti.quantity,
                       kti.status as item_status,
                       kti.hold_reason,
                       oi.order_item_id,
                       oi.snapshot_name,
                       oi.special_instruction,
                       oi.allergy_notes,
                       (
                           select string_agg(oim.name_snapshot, ', ' order by oim.created_at)
                           from order_item_modifiers oim
                           where oim.order_item_id = oi.order_item_id
                       ) as modifiers,
                       ks.kind as station_kind
                from kitchen_tickets kt
                join orders o on o.order_id = kt.order_id
                join table_sessions ts on ts.session_id = o.table_session_id
                join dining_tables dt on dt.table_id = ts.table_id
                join users u on u.user_id = o.server_user_id
                join kitchen_stations ks on ks.station_id = kt.station_id
                join kitchen_ticket_items kti on kti.ticket_id = kt.ticket_id
                join order_items oi on oi.order_item_id = kti.order_item_id
                where 1 = 1
                """;
        if (ticketId != null) {
            sql += "and kt.ticket_id = :ticketId\n";
        } else if (!includeClosedStatuses) {
            sql += """
                    and (
                        kt.status in ('queued', 'cooking', 'ready')
                        or (kt.status in ('served', 'blocked') and coalesce(kt.served_at, kt.updated_at) >= :recentCutoff)
                    )
                    """;
        }
        if (normalizedStationFilter != null) {
            sql += "and ks.kind = :stationFilter\n";
        }
        sql += """
                order by case kt.priority_label when 'expedite' then 0 when 'rush' then 1 else 2 end,
                         kt.created_at,
                         kti.created_at
                """;

        JdbcClient.StatementSpec statement = jdbcClient.sql(sql);
        if (ticketId != null) {
            statement = statement.param("ticketId", ticketId);
        }
        if (!includeClosedStatuses && ticketId == null) {
            statement = statement.param("recentCutoff", Timestamp.from(Instant.now(clock).minusSeconds(RECENT_COMPLETED_TICKET_SECONDS)));
        }
        if (normalizedStationFilter != null) {
            statement = statement.param("stationFilter", normalizedStationFilter);
        }

        LinkedHashMap<UUID, TicketAccumulator> grouped = new LinkedHashMap<>();
        statement.query((rs, rowNum) -> {
            UUID currentTicketId = rs.getObject("ticket_id", UUID.class);
            UUID orderId = rs.getObject("order_id", UUID.class);
            int tableNumber = SA.irms.common.support.TableCodeParser.parseTableNumber(rs.getString("table_code"));
            String serverName = rs.getString("display_name");
            String priority = rs.getString("priority_label");
            String ticketStatus = kitchenStatusPolicy.normalize(rs.getString("ticket_status"));
            Instant createdAt = rs.getTimestamp("created_at").toInstant();
            TicketAccumulator accumulator = grouped.computeIfAbsent(currentTicketId, ignored -> new TicketAccumulator(
                    currentTicketId,
                    orderId,
                    tableNumber,
                    serverName,
                    priority,
                    ticketStatus,
                    createdAt,
                    new ArrayList<>()
            ));
            accumulator.items().add(new KitchenViews.TicketItemView(
                    rs.getObject("ticket_item_id", UUID.class),
                    rs.getObject("order_item_id", UUID.class),
                    rs.getString("snapshot_name"),
                    rs.getInt("quantity"),
                    parseCsv(rs.getString("modifiers")),
                    rs.getString("allergy_notes"),
                    rs.getString("special_instruction"),
                    kitchenStatusPolicy.normalize(rs.getString("item_status")),
                    rs.getString("station_kind"),
                    rs.getString("hold_reason")
            ));
            return null;
        }).list();

        return grouped.values().stream()
                .map(accumulator -> new KitchenViews.TicketView(
                        accumulator.ticketId(),
                        accumulator.orderId(),
                        accumulator.tableNumber(),
                        accumulator.serverName(),
                        accumulator.priority(),
                        accumulator.status(),
                        accumulator.createdAt(),
                        accumulator.items()
                ))
                .toList();
    }

    private List<String> parseCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }

    private record TicketAccumulator(
            UUID ticketId,
            UUID orderId,
            int tableNumber,
            String serverName,
            String priority,
            String status,
            Instant createdAt,
            List<KitchenViews.TicketItemView> items
    ) {
    }
}
