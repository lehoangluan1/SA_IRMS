package SA.irms.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.inventory.application.port.out.InventoryStockTransactionRepository;

@Repository
public class JdbcInventoryStockTransactionRepository implements InventoryStockTransactionRepository {
    private final JdbcClient jdbcClient;

    public JdbcInventoryStockTransactionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public BigDecimal lockCurrentOnHand(UUID inventoryItemId) {
        return jdbcClient.sql("""
                        select on_hand
                        from inventory_items
                        where inventory_item_id = :inventoryItemId
                        for update
                        """)
                .param("inventoryItemId", inventoryItemId)
                .query(BigDecimal.class)
                .single();
    }

    @Override
    public boolean hasRecordedSource(UUID inventoryItemId, String reason, UUID sourceRef, String sourceType) {
        if (sourceRef == null || sourceType == null || sourceType.isBlank()) {
            return false;
        }
        Long existing = jdbcClient.sql("""
                        select count(*)
                        from stock_transactions
                        where inventory_item_id = :inventoryItemId
                          and source_ref = :sourceRef
                          and source_type = :sourceType
                          and reason = :reason
                        """)
                .param("inventoryItemId", inventoryItemId)
                .param("sourceRef", sourceRef)
                .param("sourceType", sourceType)
                .param("reason", reason)
                .query(Long.class)
                .single();
        return existing > 0;
    }

    @Override
    public void updateOnHand(UUID inventoryItemId, BigDecimal newOnHand) {
        jdbcClient.sql("""
                        update inventory_items
                        set on_hand = :newOnHand,
                            updated_at = now()
                        where inventory_item_id = :inventoryItemId
                        """)
                .param("newOnHand", newOnHand)
                .param("inventoryItemId", inventoryItemId)
                .update();
    }

    @Override
    public void insertTransaction(UUID transactionId, UUID inventoryItemId, BigDecimal delta, String reason,
                                  UUID sourceRef, String sourceType, BigDecimal previousOnHand, BigDecimal newOnHand,
                                  UUID performedByUserId, String correlationId, Instant occurredAt) {
        jdbcClient.sql("""
                        insert into stock_transactions (
                            transaction_id,
                            inventory_item_id,
                            delta,
                            reason,
                            source_ref,
                            source_type,
                            previous_on_hand,
                            new_on_hand,
                            performed_by_user_id,
                            correlation_id,
                            occurred_at
                        ) values (
                            :transactionId,
                            :inventoryItemId,
                            :delta,
                            :reason,
                            :sourceRef,
                            :sourceType,
                            :previousOnHand,
                            :newOnHand,
                            :performedByUserId,
                            :correlationId,
                            :occurredAt
                        )
                        """)
                .param("transactionId", transactionId)
                .param("inventoryItemId", inventoryItemId)
                .param("delta", delta)
                .param("reason", reason)
                .param("sourceRef", sourceRef)
                .param("sourceType", sourceType)
                .param("previousOnHand", previousOnHand)
                .param("newOnHand", newOnHand)
                .param("performedByUserId", performedByUserId)
                .param("correlationId", correlationId)
                .param("occurredAt", Timestamp.from(occurredAt))
                .update();
    }
}
