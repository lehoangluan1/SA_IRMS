/**
 * Domain Event Types
 *
 * These represent the event names used by the internal event bus / outbox pattern.
 * Events are consequences of committed business transactions, NOT replacements
 * for the transaction itself.
 *
 * Synchronous hot path (transactional):
 *   - open table session, check availability, browse menu, create/confirm order,
 *     create bill, process payment, close table session
 *
 * Asynchronous reactions (event-driven):
 *   - kitchen routing, status propagation, inventory consumption, low-stock alerts,
 *     notifications, receipts, audit trails, reporting projections
 */

export const DomainEvents = {
  // Table & Session
  TABLE_SESSION_OPENED: 'table.session.opened',
  TABLE_SESSION_CLOSED: 'table.session.closed',
  TABLE_STATUS_CHANGED: 'table.status.changed',

  // Reservation
  RESERVATION_CREATED: 'reservation.created',
  RESERVATION_CONFIRMED: 'reservation.confirmed',
  RESERVATION_CANCELLED: 'reservation.cancelled',
  RESERVATION_NO_SHOW: 'reservation.no_show',

  // Ordering — order confirmation is a critical transactional boundary
  ORDER_CONFIRMED: 'order.confirmed',
  ORDER_ITEM_SENT_TO_KITCHEN: 'order.item.sent_to_kitchen',
  ORDER_ITEM_CANCELLED: 'order.item.cancelled',
  ORDER_COMPLETED: 'order.completed',

  // Kitchen — asynchronous reactions to ORDER_CONFIRMED
  KITCHEN_TICKET_CREATED: 'kitchen.ticket.created',
  KITCHEN_TICKET_STARTED: 'kitchen.ticket.started',
  KITCHEN_TICKET_READY: 'kitchen.ticket.ready',
  KITCHEN_TICKET_BLOCKED: 'kitchen.ticket.blocked',

  // Billing
  BILL_CREATED: 'billing.bill.created',
  BILL_FINALIZED: 'billing.bill.finalized',
  PAYMENT_PROCESSED: 'billing.payment.processed',
  PAYMENT_FAILED: 'billing.payment.failed',
  REFUND_ISSUED: 'billing.refund.issued',

  // Inventory — asynchronous reaction to served items
  STOCK_CONSUMED: 'inventory.stock.consumed',
  LOW_STOCK_DETECTED: 'inventory.low_stock.detected',
  STOCK_REPLENISHED: 'inventory.stock.replenished',

  // Notification — asynchronous adapter
  NOTIFICATION_QUEUED: 'notification.queued',
  NOTIFICATION_SENT: 'notification.sent',
  NOTIFICATION_FAILED: 'notification.failed',

  // Receipt — asynchronous after payment confirmation
  RECEIPT_GENERATED: 'receipt.generated',
  RECEIPT_DELIVERED: 'receipt.delivered',

  // Audit — immutable trail, asynchronous pipeline
  AUDIT_LOG_RECORDED: 'audit.log.recorded',

  // Reporting — projection refresh worker
  PROJECTION_REFRESH_TRIGGERED: 'reporting.projection.refresh',
  PROJECTION_REFRESH_COMPLETED: 'reporting.projection.completed',
} as const;

export type DomainEventType = (typeof DomainEvents)[keyof typeof DomainEvents];

/**
 * Correlation ID generator for tracing transactions across boundaries.
 * In production, this would be a UUID v4 from the backend.
 */
export function generateCorrelationId(): string {
  return `corr-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}
