package SA.irms.kitchen.infrastructure.persistence;

import SA.irms.kitchen.application.port.out.KitchenAutomationActorPort;
import SA.irms.kitchen.application.port.out.KitchenTicketItemRepositoryPort;
import SA.irms.kitchen.application.port.out.KitchenTicketItemWorkflowPort;
import SA.irms.kitchen.application.query.KitchenTicketItemContext;
import SA.irms.kitchen.application.support.KitchenAutomationStep;
import java.util.List;
import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class JdbcKitchenInventoryDeductionBackfillStep implements KitchenAutomationStep {
    private final JdbcClient jdbcClient;
    private final KitchenTicketItemRepositoryPort ticketItemRepository;
    private final KitchenTicketItemWorkflowPort itemWorkflowService;
    private final KitchenAutomationActorPort priorityService;

    JdbcKitchenInventoryDeductionBackfillStep(JdbcClient jdbcClient, KitchenTicketItemRepositoryPort ticketItemRepository,
            KitchenTicketItemWorkflowPort itemWorkflowService, KitchenAutomationActorPort priorityService) {
        this.jdbcClient = jdbcClient;
        this.ticketItemRepository = ticketItemRepository;
        this.itemWorkflowService = itemWorkflowService;
        this.priorityService = priorityService;
    }

    @Override
    public void execute() {
        UUID actorUserId = priorityService.resolveAutomaticPriorityActorUserId();
        List<UUID> itemIds = jdbcClient.sql("""
                        select ticket_item_id from kitchen_ticket_items
                        where status in ('cooking', 'ready') and inventory_deducted_at is null
                        """).query(UUID.class).list();
        itemIds.forEach(ticketItemId -> {
            KitchenTicketItemContext itemContext = ticketItemRepository.loadForAutomationUpdate(ticketItemId);
            if (!List.of("cooking", "ready").contains(itemContext.status()) || itemContext.inventoryDeductedAt() != null) {
                return;
            }
            itemWorkflowService.backfillInventoryDeduction(itemContext, actorUserId);
        });
    }
}
