package SA.irms.inventory.infrastructure.persistence;

import SA.irms.inventory.application.view.InventoryViews.AlertView;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcLowStockAlertQueryRepository {
        private final JdbcClient jdbcClient;
        private final InventoryMenuImpactLookup menuImpactLookup;
        private final InventoryRelativeTimeFormatter relativeTimeFormatter;
        private final InventoryActorDisplayNameResolver actorDisplayNameResolver;

        JdbcLowStockAlertQueryRepository(
                        JdbcClient jdbcClient,
                        InventoryMenuImpactLookup menuImpactLookup,
                        InventoryRelativeTimeFormatter relativeTimeFormatter,
                        InventoryActorDisplayNameResolver actorDisplayNameResolver) {
                this.jdbcClient = jdbcClient;
                this.menuImpactLookup = menuImpactLookup;
                this.relativeTimeFormatter = relativeTimeFormatter;
                this.actorDisplayNameResolver = actorDisplayNameResolver;
        }

        List<AlertView> loadAlerts() {
                List<AlertRow> rows = jdbcClient
                                .sql("""
                                                select
                                                    a.alert_id,
                                                    i.inventory_item_id,
                                                    i.name,
                                                    coalesce(a.severity, case when i.on_hand <= i.minimum_stock / 2 then 'critical' else 'high' end) as severity,
                                                    coalesce(a.status, 'open') as status,
                                                    coalesce(a.created_at, now()) as created_at,
                                                    a.acknowledged_at,
                                                    a.acknowledged_by
                                                from inventory_items i
                                                left join low_stock_alerts a on i.inventory_item_id = a.inventory_item_id
                                                    and a.status = 'open'
                                                where i.on_hand <= i.minimum_stock
                                                order by i.on_hand / i.minimum_stock asc
                                                limit 12
                                                """)
                                .query((rs, rowNum) -> new AlertRow(
                                                rs.getObject("alert_id", UUID.class),
                                                rs.getObject("inventory_item_id", UUID.class),
                                                rs.getString("name"),
                                                rs.getString("severity"),
                                                rs.getString("status"),
                                                rs.getTimestamp("created_at").toInstant(),
                                                rs.getTimestamp("acknowledged_at") == null ? null
                                                                : rs.getTimestamp("acknowledged_at").toInstant(),
                                                rs.getObject("acknowledged_by", UUID.class)))
                                .list();
                Map<UUID, List<String>> affectedMenuItems = menuImpactLookup.loadAffectedMenuItemsByInventoryItemIds(
                                rows.stream().map(AlertRow::inventoryItemId).toList());
                Map<UUID, String> displayNames = actorDisplayNameResolver.resolveDisplayNames(
                                rows.stream().map(AlertRow::acknowledgedBy).filter(java.util.Objects::nonNull)
                                                .toList());
                return rows.stream()
                                .map(row -> new AlertView(
                                                row.alertId(),
                                                row.inventoryItemId(),
                                                row.name(),
                                                row.severity(),
                                                row.status(),
                                                relativeTimeFormatter.format(row.createdAt()),
                                                row.acknowledgedAt() == null ? null
                                                                : relativeTimeFormatter.format(row.acknowledgedAt()),
                                                actorDisplayNameResolver.resolveDisplayName(row.acknowledgedBy(),
                                                                displayNames),
                                                affectedMenuItems.getOrDefault(row.inventoryItemId(), List.of())))
                                .toList();
        }

        private record AlertRow(
                        UUID alertId,
                        UUID inventoryItemId,
                        String name,
                        String severity,
                        String status,
                        Instant createdAt,
                        Instant acknowledgedAt,
                        UUID acknowledgedBy) {
        }
}