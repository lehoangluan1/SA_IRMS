package SA.irms.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import SA.irms.inventory.application.port.out.ReorderSuggestionCommandPort;

@Repository
public class JdbcReorderSuggestionAdapter implements ReorderSuggestionCommandPort {
    private final JdbcClient jdbcClient;

    public JdbcReorderSuggestionAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void upsertSuggestion(UUID inventoryItemId, BigDecimal threshold, BigDecimal targetQuantity, int leadTimeDays) {
        jdbcClient.sql("""
                        insert into reorder_rules (rule_id, inventory_item_id, threshold, target_qty, lead_time_days, is_active)
                        values (:ruleId, :inventoryItemId, :threshold, :targetQty, :leadTimeDays, true)
                        on conflict (inventory_item_id) do update
                        set threshold = excluded.threshold,
                            target_qty = greatest(reorder_rules.target_qty, excluded.target_qty),
                            lead_time_days = excluded.lead_time_days,
                            is_active = true
                        """)
                .param("ruleId", UUID.randomUUID())
                .param("inventoryItemId", inventoryItemId)
                .param("threshold", threshold)
                .param("targetQty", targetQuantity)
                .param("leadTimeDays", leadTimeDays)
                .update();
    }
}
