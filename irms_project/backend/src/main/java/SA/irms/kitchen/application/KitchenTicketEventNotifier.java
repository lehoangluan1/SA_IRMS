package SA.irms.kitchen.application;

import SA.irms.kitchen.application.query.KitchenTicketItemContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import SA.irms.common.outbox.DomainEventPublisher;
import SA.irms.kitchen.application.events.KitchenDishStatusChangedEvent;
import SA.irms.common.notification.NotificationCommandPublisher;
import SA.irms.common.notification.NotificationCommand;

@Service
public class KitchenTicketEventNotifier {
    private final DomainEventPublisher outboxEventPublisher;
    private final NotificationCommandPublisher notificationOutboxPublisher;

    KitchenTicketEventNotifier(DomainEventPublisher outboxEventPublisher, NotificationCommandPublisher notificationOutboxPublisher) {
        this.outboxEventPublisher = outboxEventPublisher;
        this.notificationOutboxPublisher = notificationOutboxPublisher;
    }

    void publishDishStatus(KitchenTicketItemContext item, UUID handoffId, String type, Map<String, Object> details) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ticketItemId", item.ticketItemId().toString());
        payload.put("type", type);
        payload.put("sourceStation", item.stationKind());
        payload.put("details", details == null ? Map.of() : details);
        if (handoffId != null) {
            payload.put("handoffId", handoffId.toString());
        }
        payload.put("ticketId", item.ticketId().toString());
        payload.put("status", type);
        outboxEventPublisher.publish(new KitchenDishStatusChangedEvent(
                item.ticketItemId().toString(),
                payload
        ), "kitchen-ticket-" + item.ticketId(), null);
    }

    void queueReadyNotification(KitchenTicketItemContext item) {
        notificationOutboxPublisher.enqueue(new NotificationCommand(
                null,
                null,
                null,
                null,
                "in_app",
                "kitchen_status",
                "KITCHEN_ITEM_READY",
                Map.of(
                        "ticketItemId", item.ticketItemId().toString(),
                        "tableCode", item.tableCode(),
                        "dishName", item.dishName()
                ),
                "Dish ready for service",
                item.dishName() + " for " + item.tableCode() + " is ready for pickup.",
                null,
                item.serverUserId(),
                item.tableCode(),
                "medium"
        ), "KitchenTicketItem", item.ticketItemId().toString(), "kitchen-ticket-" + item.ticketId());
    }
}
