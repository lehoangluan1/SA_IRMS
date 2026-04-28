package SA.irms.kitchen.infrastructure.persistence;

import SA.irms.kitchen.application.port.out.KitchenTicketItemRepositoryPort;
import SA.irms.kitchen.application.port.out.KitchenTicketItemWorkflowPort;
import SA.irms.kitchen.application.port.out.KitchenTicketStateCoordinatorPort;
import SA.irms.kitchen.application.query.KitchenTicketItemContext;
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
@Order(40)
public class JdbcKitchenCookingItemAutoAdvancer implements KitchenAutomationStep {
    private final JdbcClient jdbcClient;
    private final KitchenTicketItemRepositoryPort ticketItemRepository;
    private final KitchenTicketItemWorkflowPort itemWorkflowService;
    private final KitchenTicketStateCoordinatorPort ticketStateCoordinator;
    private final Clock clock;

    JdbcKitchenCookingItemAutoAdvancer(JdbcClient jdbcClient, KitchenTicketItemRepositoryPort ticketItemRepository,
            KitchenTicketItemWorkflowPort itemWorkflowService, KitchenTicketStateCoordinatorPort ticketStateCoordinator, Clock clock) {
        this.jdbcClient = jdbcClient;
        this.ticketItemRepository = ticketItemRepository;
        this.itemWorkflowService = itemWorkflowService;
        this.ticketStateCoordinator = ticketStateCoordinator;
        this.clock = clock;
    }

    @Override
    public void execute() {
        Instant now = Instant.now(clock);
        List<UUID> itemIds = jdbcClient.sql("""
                        select ticket_item_id from kitchen_ticket_items
                        where status = 'cooking' and next_action_at is not null and next_action_at <= :cutoff
                        """)
                .param("cutoff", Timestamp.from(now))
                .query(UUID.class)
                .list();
        itemIds.forEach(ticketItemId -> {
            KitchenTicketItemContext itemContext = ticketItemRepository.loadForAutomationUpdate(ticketItemId);
            if (!"cooking".equals(itemContext.status())) {
                return;
            }
            itemWorkflowService.markItemReady(itemContext);
            ticketStateCoordinator.reconcileTicket(itemContext.ticketId());
        });
    }
}
