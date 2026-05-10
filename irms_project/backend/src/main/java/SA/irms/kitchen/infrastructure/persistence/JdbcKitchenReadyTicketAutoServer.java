package SA.irms.kitchen.infrastructure.persistence;

import SA.irms.common.security.AuthenticatedUser;
import SA.irms.kitchen.application.port.out.KitchenAutomationActorPort;
import SA.irms.kitchen.application.port.out.KitchenTicketAutoServePort;
import SA.irms.kitchen.application.support.KitchenAutomationStep;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Order(60)
public class JdbcKitchenReadyTicketAutoServer implements KitchenAutomationStep {
    private final JdbcClient jdbcClient;
    private final KitchenTicketAutoServePort handoffService;
    private final KitchenAutomationActorPort priorityService;
    private final Clock clock;

    JdbcKitchenReadyTicketAutoServer(JdbcClient jdbcClient, KitchenTicketAutoServePort handoffService,
            KitchenAutomationActorPort priorityService, Clock clock) {
        this.jdbcClient = jdbcClient;
        this.handoffService = handoffService;
        this.priorityService = priorityService;
        this.clock = clock;
    }

    @Override
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
