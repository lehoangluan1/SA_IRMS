package SA.irms.kitchen.application;

import SA.irms.kitchen.application.support.KitchenTimingPolicy;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Order(10)
class KitchenQueuedItemScheduler implements KitchenAutomationStep {
    private final JdbcClient jdbcClient;
    private final KitchenTimingPolicy timingPolicy;
    private final Clock clock;

    KitchenQueuedItemScheduler(JdbcClient jdbcClient, KitchenTimingPolicy timingPolicy, Clock clock) {
        this.jdbcClient = jdbcClient;
        this.timingPolicy = timingPolicy;
        this.clock = clock;
    }

    public void execute() {
        Instant now = Instant.now(clock);
        List<UUID> itemIds = jdbcClient.sql("""
                        select ticket_item_id from kitchen_ticket_items
                        where status = 'queued' and next_action_at is null
                        """).query(UUID.class).list();
        itemIds.forEach(ticketItemId -> jdbcClient.sql("""
                        update kitchen_ticket_items
                        set next_action_at = :nextActionAt
                        where ticket_item_id = :ticketItemId and status = 'queued' and next_action_at is null
                        """)
                .param("nextActionAt", Timestamp.from(now.plusSeconds(timingPolicy.randomQueueDelaySeconds())))
                .param("ticketItemId", ticketItemId)
                .update());
    }
}
