package SA.irms.common.events;

import java.util.List;
import java.util.Map;

public final class EventRoutingRegistry {
    public static final String ORDER_EXCHANGE = "irms.order.exchange";
    public static final String KITCHEN_EXCHANGE = "irms.kitchen.exchange";
    public static final String INVENTORY_EXCHANGE = "irms.inventory.exchange";
    public static final String BILLING_EXCHANGE = "irms.billing.exchange";
    public static final String RESERVATION_EXCHANGE = "irms.reservation.exchange";
    public static final String NOTIFICATION_EXCHANGE = "irms.notification.exchange";
    public static final String AUDIT_EXCHANGE = "irms.audit.exchange";
    public static final String REPORTING_EXCHANGE = "irms.reporting.exchange";
    public static final String DLX_EXCHANGE = "irms.dlx.exchange";
    public static final String RETRY_EXCHANGE = "irms.retry.exchange";

    private static final Map<String, EventRouting> ROUTING = Map.ofEntries(
            Map.entry(ServiceEventTypes.ORDER_CONFIRMED, new EventRouting(ORDER_EXCHANGE, "order.confirmed")),
            Map.entry(ServiceEventTypes.ORDER_CANCELLED, new EventRouting(ORDER_EXCHANGE, "order.cancelled")),
            Map.entry(ServiceEventTypes.KITCHEN_TICKET_CREATED, new EventRouting(KITCHEN_EXCHANGE, "kitchen.ticket.created")),
            Map.entry(ServiceEventTypes.KITCHEN_DISH_STATUS_CHANGED, new EventRouting(KITCHEN_EXCHANGE, "kitchen.dish.status.changed")),
            Map.entry(ServiceEventTypes.INVENTORY_STOCK_CHANGED, new EventRouting(INVENTORY_EXCHANGE, "inventory.stock.changed")),
            Map.entry(ServiceEventTypes.LOW_STOCK_DETECTED, new EventRouting(INVENTORY_EXCHANGE, "inventory.low-stock.detected")),
            Map.entry(ServiceEventTypes.BILL_ISSUED, new EventRouting(BILLING_EXCHANGE, "billing.bill.issued")),
            Map.entry(ServiceEventTypes.PAYMENT_COMPLETED, new EventRouting(BILLING_EXCHANGE, "billing.payment.completed")),
            Map.entry(ServiceEventTypes.REFUND_ISSUED, new EventRouting(BILLING_EXCHANGE, "billing.refund.issued")),
            Map.entry(ServiceEventTypes.RECEIPT_GENERATED, new EventRouting(BILLING_EXCHANGE, "billing.receipt.generated")),
            Map.entry(ServiceEventTypes.NOTIFICATION_REQUESTED, new EventRouting(NOTIFICATION_EXCHANGE, "notification.requested")),
            Map.entry(ServiceEventTypes.AUDIT_RECORDING_REQUESTED, new EventRouting(AUDIT_EXCHANGE, "audit.recording.requested")),
            Map.entry(ServiceEventTypes.AUDIT_FOLLOW_UP_REQUESTED, new EventRouting(AUDIT_EXCHANGE, "audit.follow-up.requested")),
            Map.entry(ServiceEventTypes.SESSION_TOUCH_REQUESTED, new EventRouting(AUDIT_EXCHANGE, "session.touch.requested")),
            Map.entry(ServiceEventTypes.RESERVATION_CREATED, new EventRouting(RESERVATION_EXCHANGE, "reservation.created")),
            Map.entry(ServiceEventTypes.RESERVATION_SEATED, new EventRouting(RESERVATION_EXCHANGE, "reservation.seated")),
            Map.entry(ServiceEventTypes.WAITLIST_UPDATED, new EventRouting(RESERVATION_EXCHANGE, "reservation.waitlisted")),
            Map.entry(ServiceEventTypes.MENU_ITEM_AVAILABILITY_CHANGED, new EventRouting(ORDER_EXCHANGE, "menu.item.availability.changed")),
            Map.entry(ServiceEventTypes.PROMOTION_UPDATED, new EventRouting(ORDER_EXCHANGE, "promotion.updated")),
            Map.entry(ServiceEventTypes.STAFF_SHIFT_CHANGED, new EventRouting(AUDIT_EXCHANGE, "staff.shift.changed")),
            Map.entry(ServiceEventTypes.BILLING_SETTLEMENT_PROJECTED, new EventRouting(REPORTING_EXCHANGE, "reporting.billing.settlement.projected"))
    );

    private static final Map<String, List<String>> CONSUMER_QUEUES_BY_EVENT_TYPE = Map.ofEntries(
            Map.entry(ServiceEventTypes.ORDER_CONFIRMED, List.of(
                    "irms.order-fulfillment.order-confirmed.q",
                    "irms.reporting.order-confirmed.q",
                    "irms.audit.order-confirmed.q"
            )),
            Map.entry(ServiceEventTypes.ORDER_CANCELLED, List.of(
                    "irms.order-fulfillment.order-cancelled.q",
                    "irms.kitchen.order-cancelled.q",
                    "irms.inventory.order-cancelled.q",
                    "irms.billing.order-cancelled.q",
                    "irms.audit.order-cancelled.q",
                    "irms.notification.order-cancelled.q",
                    "irms.reporting.order-cancelled.q"
            )),
            Map.entry(ServiceEventTypes.KITCHEN_TICKET_CREATED, List.of(
                    "irms.reporting.kitchen-ticket-created.q",
                    "irms.audit.kitchen-ticket-created.q"
            )),
            Map.entry(ServiceEventTypes.KITCHEN_DISH_STATUS_CHANGED, List.of(
                    "irms.order-fulfillment.kitchen-dish-status.q",
                    "irms.kitchen.dish-status.workflow.q",
                    "irms.reporting.kitchen-dish-status.q",
                    "irms.notification.kitchen-dish-status.q",
                    "irms.audit.kitchen-dish-status.q"
            )),
            Map.entry(ServiceEventTypes.INVENTORY_STOCK_CHANGED, List.of(
                    "irms.inventory.low-stock.q",
                    "irms.reporting.inventory-stock.q",
                    "irms.inventory.reorder-suggestion.q",
                    "irms.audit.inventory-stock.q"
            )),
            Map.entry(ServiceEventTypes.LOW_STOCK_DETECTED, List.of(
                    "irms.notification.low-stock.q",
                    "irms.reporting.low-stock.q",
                    "irms.audit.low-stock.q"
            )),
            Map.entry(ServiceEventTypes.BILL_ISSUED, List.of(
                    "irms.reporting.bill-issued.q",
                    "irms.audit.bill-issued.q"
            )),
            Map.entry(ServiceEventTypes.PAYMENT_COMPLETED, List.of(
                    "irms.billing.receipt.q",
                    "irms.reporting.payment-completed.q",
                    "irms.audit.payment-completed.q",
                    "irms.notification.payment-completed.q"
            )),
            Map.entry(ServiceEventTypes.REFUND_ISSUED, List.of(
                    "irms.billing.refund-settlement.q",
                    "irms.reporting.refund-issued.q",
                    "irms.audit.refund-issued.q",
                    "irms.notification.refund-issued.q"
            )),
            Map.entry(ServiceEventTypes.RECEIPT_GENERATED, List.of(
                    "irms.reporting.receipt-generated.q",
                    "irms.notification.receipt-generated.q",
                    "irms.audit.receipt-generated.q"
            )),
            Map.entry(ServiceEventTypes.BILLING_SETTLEMENT_PROJECTED, List.of(
                    "irms.reporting.billing-settlement.q"
            )),
            Map.entry(ServiceEventTypes.NOTIFICATION_REQUESTED, List.of(
                    "irms.notification.requested.q"
            )),
            Map.entry(ServiceEventTypes.AUDIT_RECORDING_REQUESTED, List.of(
                    "irms.audit.recording-requested.q"
            )),
            Map.entry(ServiceEventTypes.AUDIT_FOLLOW_UP_REQUESTED, List.of(
                    "irms.notification.audit-follow-up.q"
            )),
            Map.entry(ServiceEventTypes.SESSION_TOUCH_REQUESTED, List.of(
                    "irms.audit.session-touch.q"
            )),
            Map.entry(ServiceEventTypes.RESERVATION_CREATED, List.of(
                    "irms.reporting.reservation-created.q",
                    "irms.audit.reservation-created.q",
                    "irms.notification.reservation-created.q"
            )),
            Map.entry(ServiceEventTypes.RESERVATION_SEATED, List.of(
                    "irms.reporting.reservation-seated.q",
                    "irms.notification.reservation-seated.q",
                    "irms.audit.reservation-seated.q"
            )),
            Map.entry(ServiceEventTypes.WAITLIST_UPDATED, List.of(
                    "irms.reporting.waitlist-updated.q",
                    "irms.notification.waitlist-updated.q",
                    "irms.audit.waitlist-updated.q"
            )),
            Map.entry(ServiceEventTypes.MENU_ITEM_AVAILABILITY_CHANGED, List.of(
                    "irms.reporting.menu-availability.q",
                    "irms.audit.menu-availability.q",
                    "irms.notification.menu-availability.q"
            )),
            Map.entry(ServiceEventTypes.PROMOTION_UPDATED, List.of(
                    "irms.reporting.promotion-updated.q",
                    "irms.audit.promotion-updated.q",
                    "irms.notification.promotion-updated.q"
            )),
            Map.entry(ServiceEventTypes.STAFF_SHIFT_CHANGED, List.of(
                    "irms.reporting.staff-shift.q",
                    "irms.audit.staff-shift.q"
            ))
    );

    private EventRoutingRegistry() {
    }

    public static EventRouting routingFor(String eventType) {
        EventRouting routing = ROUTING.get(eventType);
        if (routing == null) {
            throw new IllegalArgumentException("No RabbitMQ routing is configured for event type " + eventType + ".");
        }
        return routing;
    }

    public static Map<String, EventRouting> routing() {
        return ROUTING;
    }

    public static List<String> queuesFor(String eventType) {
        return CONSUMER_QUEUES_BY_EVENT_TYPE.getOrDefault(eventType, List.of());
    }
}
