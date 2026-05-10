package SA.irms.common.events;

public final class ServiceEventTypes {
    private ServiceEventTypes() {
    }

    public static final String ORDER_CONFIRMED = "OrderConfirmed";
    public static final String ORDER_CANCELLED = "OrderCancelled";
    public static final String KITCHEN_TICKET_CREATED = "KitchenTicketCreated";
    public static final String KITCHEN_DISH_STATUS_CHANGED = "KitchenDishStatusChanged";
    public static final String INVENTORY_STOCK_CHANGED = "InventoryStockChanged";
    public static final String LOW_STOCK_DETECTED = "LowStockDetected";
    public static final String BILL_ISSUED = "BillIssued";
    public static final String PAYMENT_COMPLETED = "PaymentCompleted";
    public static final String REFUND_ISSUED = "RefundIssued";
    public static final String RECEIPT_GENERATED = "ReceiptGenerated";
    public static final String RESERVATION_CREATED = "ReservationCreated";
    public static final String RESERVATION_SEATED = "ReservationSeated";
    public static final String WAITLIST_UPDATED = "WaitlistUpdated";
    public static final String MENU_ITEM_AVAILABILITY_CHANGED = "MenuItemAvailabilityChanged";
    public static final String PROMOTION_UPDATED = "PromotionUpdated";
    public static final String NOTIFICATION_REQUESTED = "NotificationRequested";
    public static final String AUDIT_RECORDING_REQUESTED = "AuditRecordingRequested";
    public static final String AUDIT_FOLLOW_UP_REQUESTED = "AuditFollowUpRequested";
    public static final String SESSION_TOUCH_REQUESTED = "SessionTouchRequested";
    public static final String STAFF_SHIFT_CHANGED = "StaffShiftChanged";
    public static final String BILLING_SETTLEMENT_PROJECTED = "BillingSettlementProjected";
}
