package SA.irms.kitchen.application;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import SA.irms.common.security.AuthenticatedUser;

@Component
@Order(60)
class KitchenReadyTicketAutoServer implements KitchenAutomationStep {
    private final JdbcClient jdbcClient;
    private final KitchenTicketHandoffService handoffService;
    private final KitchenPriorityService priorityService;
    private final Clock clock;

    KitchenReadyTicketAutoServer(JdbcClient jdbcClient, KitchenTicketHandoffService handoffService,
            KitchenPriorityService priorityService, Clock clock) {
        this.jdbcClient = jdbcClient;
        this.handoffService = handoffService;
        this.priorityService = priorityService;
        this.clock = clock;
    }

    public void execute() {
        Instant now = Instant.now(clock);
        List<UUID> ticketIds = jdbcClient.sql("""
                        select ticket_id from kitchen_tickets
                        where status = 'ready' and next_action_at is not null and next_action_at <= :cutoff
                        """)
                .param("cutoff", Timestamp.from(now))
                .query(UUID.class)
                .list();
        AuthenticatedUser actor = priorityService.automaticKitchenActor();
        ticketIds.forEach(ticketId -> handoffService.autoServeReadyTicket(ticketId, actor));
    }
}
