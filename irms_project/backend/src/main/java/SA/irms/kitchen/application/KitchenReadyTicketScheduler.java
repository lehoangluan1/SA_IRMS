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
@Order(50)
class KitchenReadyTicketScheduler implements KitchenAutomationStep {
    private final JdbcClient jdbcClient;
    private final KitchenTimingPolicy timingPolicy;
    private final Clock clock;

    KitchenReadyTicketScheduler(JdbcClient jdbcClient, KitchenTimingPolicy timingPolicy, Clock clock) {
        this.jdbcClient = jdbcClient;
        this.timingPolicy = timingPolicy;
        this.clock = clock;
    }

    public void execute() {
        Instant now = Instant.now(clock);
        List<UUID> ticketIds = jdbcClient.sql("""
                        select ticket_id from kitchen_tickets
                        where status = 'ready' and next_action_at is null
                        """).query(UUID.class).list();
        ticketIds.forEach(ticketId -> jdbcClient.sql("""
                        update kitchen_tickets
                        set next_action_at = :nextActionAt
                        where ticket_id = :ticketId and status = 'ready' and next_action_at is null
                        """)
                .param("nextActionAt", Timestamp.from(now.plusSeconds(timingPolicy.randomReadyToServedSeconds())))
                .param("ticketId", ticketId)
                .update());
    }
}
