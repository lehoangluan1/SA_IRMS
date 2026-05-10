package SA.irms.kitchen.application.workflow;

import java.util.Map;

import org.springframework.stereotype.Component;

import SA.irms.kitchen.domain.KitchenTicketState;
import SA.irms.common.outbox.DomainEventPublisher;

@Component
public class KitchenInventoryUsageStep implements KitchenWorkflowStep<KitchenTicketTransitionContext> {
    private final DomainEventPublisher outboxPublisher;

    public KitchenInventoryUsageStep(DomainEventPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    @Override
    public String name() {
        return "KITCHEN_INVENTORY_USAGE_RESERVED";
    }

    @Override
    public void execute(KitchenTicketTransitionContext context) {
        if (context.targetState() != KitchenTicketState.COOKING) {
            return;
        }
        outboxPublisher.publish("InventoryStockChanged", 1, "InventoryItem", context.ticketId().toString(), Map.of(
                "source", "KitchenWorkflowMediator",
                "ticketId", context.ticketId().toString(),
                "reason", "INGREDIENT_USAGE_RESERVED"
        ), "kitchen-ticket-" + context.ticketId(), null, null);
    }
}
