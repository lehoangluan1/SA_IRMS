package SA.irms.common.events;

public final class ServiceEventConsumers {
    private ServiceEventConsumers() {
    }

    public static final String NOTIFICATION_REQUEST_DELIVERY = "notification.notification-request.delivery";
    public static final String AUDIT_FOLLOW_UP_ROUTER = "notification.audit-follow-up.router";
    public static final String AUDIT_RECORDING_MATERIALIZER = "identity.audit-recording.materializer";
    public static final String SESSION_TOUCH_MATERIALIZER = "identity.session-touch.materializer";
    public static final String INVENTORY_LOW_STOCK_EVALUATOR = "inventory.low-stock.evaluator";
    public static final String KITCHEN_DISH_STATUS_PERSISTENCE = "kitchen.dish-status.persistence";
    public static final String REPORTING_INVENTORY_PROJECTION = "reporting.inventory.projection";
    public static final String REPORTING_KITCHEN_PROJECTION = "reporting.kitchen.projection";
    public static final String LEGACY_RESERVATION_CONFIRMED_ACK = "common.legacy.reservation-confirmed.ack";
    public static final String LEGACY_INVENTORY_LOW_ACK = "common.legacy.inventory-low.ack";
}
