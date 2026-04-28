package SA.irms.kitchen.application;

import SA.irms.kitchen.application.query.KitchenTicketItemContext;
import SA.irms.kitchen.application.support.KitchenTimingPolicy;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import SA.irms.kitchen.application.port.out.KitchenInventoryConsumptionPort;
import SA.irms.kitchen.application.port.out.KitchenTicketItemCommandPort;

@Service
public class KitchenTicketItemWorkflowService implements SA.irms.kitchen.application.port.out.KitchenTicketItemWorkflowPort {
    private final KitchenTicketItemCommandPort itemCommandPort;
    private final KitchenInventoryConsumptionPort inventoryService;
    private final KitchenTicketEventNotifier eventNotifier;
    private final KitchenTimingPolicy timingPolicy;
    private final Clock clock;

    public KitchenTicketItemWorkflowService(
            KitchenTicketItemCommandPort itemCommandPort,
            KitchenInventoryConsumptionPort inventoryService,
            KitchenTicketEventNotifier eventNotifier,
            KitchenTimingPolicy timingPolicy,
            Clock clock
    ) {
        this.itemCommandPort = itemCommandPort;
        this.inventoryService = inventoryService;
        this.eventNotifier = eventNotifier;
        this.timingPolicy = timingPolicy;
        this.clock = clock;
    }

    public void moveItemToCooking(KitchenTicketItemContext itemContext, UUID actorUserId, String correlationId) {
        inventoryService.consumeForKitchenStart(itemContext.orderItemId(), actorUserId, correlationId);
        itemCommandPort.moveItemToCooking(
                itemContext.ticketItemId(),
                Instant.now(clock).plusSeconds(timingPolicy.randomCookingSeconds())
        );
        eventNotifier.publishDishStatus(itemContext, null, "cooking", Map.of("ticketId", itemContext.ticketId().toString()));
    }

    public void markItemReady(KitchenTicketItemContext itemContext) {
        itemCommandPort.markItemReady(itemContext.ticketItemId());
        eventNotifier.publishDishStatus(itemContext, null, "ready", Map.of("ticketId", itemContext.ticketId().toString()));
        eventNotifier.queueReadyNotification(itemContext);
    }

    public void blockItem(KitchenTicketItemContext itemContext, String reason) {
        itemCommandPort.blockItem(itemContext.ticketItemId(), reason);
        eventNotifier.publishDishStatus(itemContext, null, "blocked", Map.of(
                "ticketId", itemContext.ticketId().toString(),
                "reason", reason
        ));
    }

    public void backfillInventoryDeduction(KitchenTicketItemContext itemContext, UUID actorUserId) {
        inventoryService.consumeForKitchenStart(itemContext.orderItemId(), actorUserId, "system:inventory-backfill");
        itemCommandPort.markInventoryDeducted(itemContext.ticketItemId());
    }
}
