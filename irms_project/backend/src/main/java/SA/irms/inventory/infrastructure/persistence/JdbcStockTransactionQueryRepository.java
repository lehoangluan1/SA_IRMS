package SA.irms.inventory.infrastructure.persistence;

import SA.irms.inventory.application.view.InventoryViews.TransactionView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcStockTransactionQueryRepository {
    private final JdbcClient jdbcClient;
    private final InventoryActorDisplayNameResolver actorDisplayNameResolver;
    private final InventoryRelativeTimeFormatter relativeTimeFormatter;

    JdbcStockTransactionQueryRepository(
            JdbcClient jdbcClient,
            InventoryActorDisplayNameResolver actorDisplayNameResolver,
            InventoryRelativeTimeFormatter relativeTimeFormatter
    ) {
        this.jdbcClient = jdbcClient;
        this.actorDisplayNameResolver = actorDisplayNameResolver;
        this.relativeTimeFormatter = relativeTimeFormatter;
    }

    List<TransactionView> loadTransactions() {
        List<TransactionRow> rows = jdbcClient.sql("""
                        select t.transaction_id, i.name, t.reason, t.delta, t.performed_by_user_id, t.source_type, t.occurred_at
                        from stock_transactions t
                        join inventory_items i on i.inventory_item_id = t.inventory_item_id
                        order by t.occurred_at desc
                       
                        """)
                .query((rs, rowNum) -> new TransactionRow(
                        rs.getObject("transaction_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("reason"),
                        rs.getBigDecimal("delta"),
                        rs.getObject("performed_by_user_id", UUID.class),
                        rs.getString("source_type"),
                        rs.getTimestamp("occurred_at").toInstant()
                ))
                .list();
        Map<UUID, String> names = actorDisplayNameResolver.resolveDisplayNames(
                rows.stream().map(TransactionRow::performedByUserId).filter(java.util.Objects::nonNull).toList()
        );
        return rows.stream()
                .map(row -> new TransactionView(
                        row.transactionId(),
                        row.name(),
                        row.reason(),
                        row.delta(),
                        actorDisplayNameResolver.resolveActor(row.performedByUserId(), row.sourceType(), names),
                        relativeTimeFormatter.format(row.occurredAt())
                ))
                .toList();
    }

    private record TransactionRow(
            UUID transactionId,
            String name,
            String reason,
            BigDecimal delta,
            UUID performedByUserId,
            String sourceType,
            Instant occurredAt
    ) {
    }
}
