// ========================================
// Table & Reservation Domain
// ========================================

export type TableStatus = 'available' | 'reserved' | 'occupied' | 'cleaning' | 'out_of_service';

export interface Table {
  id: string;
  number: number;
  capacity: number;
  status: TableStatus;
  floor: string;
  section: string;
  position: { row: number; col: number };
  activeSessionId?: string;
}

export interface TableSession {
  id: string;
  tableId: string;
  tableNumber: number;
  serverId: string;
  serverName: string;
  guestCount: number;
  startedAt: string;
  closedAt?: string;
  status: 'active' | 'billing' | 'closed';
  reservationId?: string;
  correlationId: string;
}

export interface Reservation {
  id: string;
  guestName: string;
  guestPhone: string;
  guestEmail?: string;
  partySize: number;
  date: string;
  time: string;
  status: 'pending' | 'confirmed' | 'seated' | 'completed' | 'cancelled' | 'no_show';
  tableId?: string;
  notes?: string;
  createdAt: string;
}

export interface WaitlistEntry {
  id: string;
  guestName: string;
  partySize: number;
  estimatedWait: number;
  addedAt: string;
  status: 'waiting' | 'notified' | 'seated' | 'left';
  phone: string;
}

// ========================================
// Menu Domain
// ========================================

export interface MenuCategory {
  id: string;
  name: string;
  sortOrder: number;
  isActive: boolean;
}

export interface MenuItem {
  id: string;
  name: string;
  description: string;
  price: number;
  categoryId: string;
  categoryName: string;
  station: KitchenStationType;
  isAvailable: boolean;
  allergens: string[];
  modifierGroups: ModifierGroup[];
  preparationTime: number;
}

export interface ModifierGroup {
  id: string;
  name: string;
  required: boolean;
  multiSelect: boolean;
  options: ModifierOption[];
}

export interface ModifierOption {
  id: string;
  name: string;
  priceAdjustment: number;
}

// ========================================
// Ordering Domain
// ========================================

export type OrderStatus = 'draft' | 'confirmed' | 'in_progress' | 'ready' | 'served' | 'cancelled';

export interface Order {
  id: string;
  tableSessionId: string;
  tableNumber: number;
  serverId: string;
  serverName: string;
  status: OrderStatus;
  items: OrderItem[];
  createdAt: string;
  confirmedAt?: string;
  totalAmount: number;
  specialInstructions?: string;
  correlationId: string;
}

export interface OrderItem {
  id: string;
  menuItemId: string;
  menuItemName: string;
  quantity: number;
  unitPrice: number;
  modifiers: SelectedModifier[];
  allergyNotes?: string;
  specialInstructions?: string;
  status: 'pending' | 'sent_to_kitchen' | 'cooking' | 'ready' | 'served' | 'cancelled';
  cancellationReason?: string;
}

export interface SelectedModifier {
  modifierOptionId: string;
  name: string;
  priceAdjustment: number;
}

// ========================================
// Kitchen Domain
// ========================================

export type KitchenStationType = 'grill' | 'fryer' | 'dessert' | 'drinks' | 'salad' | 'prep';

export type KitchenItemStatus = 'new' | 'started' | 'cooking' | 'ready' | 'blocked' | 'hold_for_service';

export interface KitchenTicket {
  id: string;
  orderId: string;
  tableNumber: number;
  serverName: string;
  station: KitchenStationType;
  items: KitchenTicketItem[];
  priority: 'normal' | 'rush' | 'expedite';
  status: KitchenItemStatus;
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
}

export interface KitchenTicketItem {
  id: string;
  orderItemId: string;
  name: string;
  quantity: number;
  modifiers: string[];
  allergyNotes?: string;
  specialInstructions?: string;
  status: KitchenItemStatus;
}

// ========================================
// Billing Domain
// ========================================

export type BillStatus = 'open' | 'finalized' | 'paid' | 'partially_paid' | 'refunded' | 'void';

export interface Bill {
  id: string;
  tableSessionId: string;
  tableNumber: number;
  orderIds: string[];
  subtotal: number;
  taxAmount: number;
  taxRate: number;
  serviceFee: number;
  discount: number;
  promotionCode?: string;
  tipAmount: number;
  total: number;
  status: BillStatus;
  splits: BillSplit[];
  createdAt: string;
  finalizedAt?: string;
}

export interface BillSplit {
  id: string;
  billId: string;
  label: string;
  amount: number;
  status: 'pending' | 'paid';
  paymentId?: string;
}

export type PaymentMethod = 'cash' | 'credit_card' | 'debit_card' | 'mobile_payment' | 'gift_card';

export interface Payment {
  id: string;
  billId: string;
  amount: number;
  method: PaymentMethod;
  status: 'pending' | 'completed' | 'failed' | 'refunded';
  externalTransactionRef?: string;
  processedAt: string;
  processedBy: string;
}

export interface Refund {
  id: string;
  paymentId: string;
  billId: string;
  amount: number;
  reason: string;
  approvedBy: string;
  processedAt: string;
  externalTransactionRef?: string;
}

export interface Receipt {
  id: string;
  billId: string;
  format: 'print' | 'email' | 'sms';
  sentAt: string;
  recipientAddress?: string;
}

// ========================================
// Inventory Domain
// ========================================

export interface StockItem {
  id: string;
  name: string;
  unit: string;
  currentStock: number;
  minimumStock: number;
  maximumStock: number;
  costPerUnit: number;
  category: string;
  lastRestocked: string;
  affectedMenuItems: string[];
}

export interface StockTransaction {
  id: string;
  stockItemId: string;
  stockItemName: string;
  type: 'consumption' | 'restock' | 'manual_adjustment' | 'waste' | 'return';
  quantity: number;
  previousStock: number;
  newStock: number;
  reason?: string;
  performedBy: string;
  performedAt: string;
  correlationId?: string;
}

export interface LowStockRule {
  id: string;
  stockItemId: string;
  stockItemName: string;
  threshold: number;
  action: 'alert' | 'hide_menu_item' | 'auto_reorder';
  isActive: boolean;
}

// ========================================
// IAM Domain
// ========================================

export type UserRole = 'admin' | 'manager' | 'server' | 'chef' | 'cashier' | 'host';

export interface Staff {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  isActive: boolean;
  avatarUrl?: string;
  hireDate: string;
  permissions: string[];
}

export interface ShiftAssignment {
  id: string;
  staffId: string;
  staffName: string;
  role: UserRole;
  date: string;
  startTime: string;
  endTime: string;
  station?: string;
}

// ========================================
// Audit Domain
// ========================================

export interface AuditLog {
  id: string;
  timestamp: string;
  actorId: string;
  actorName: string;
  actorRole: UserRole;
  action: string;
  entityType: string;
  entityId: string;
  correlationId: string;
  before?: Record<string, unknown>;
  after?: Record<string, unknown>;
  reason?: string;
  followUp: boolean;
  ipAddress: string;
}

// ========================================
// Notification Domain
// ========================================

export interface NotificationMessage {
  id: string;
  type: 'low_stock' | 'order_ready' | 'reservation_reminder' | 'payment_received' | 'anomaly_detected';
  title: string;
  body: string;
  recipientRole?: UserRole;
  recipientId?: string;
  createdAt: string;
  readAt?: string;
  priority: 'low' | 'medium' | 'high' | 'critical';
}

// ========================================
// Event System (Outbox / Internal Bus)
// ========================================

export interface DomainEvent {
  id: string;
  type: string;
  aggregateId: string;
  aggregateType: string;
  payload: Record<string, unknown>;
  occurredAt: string;
  correlationId: string;
  processedAt?: string;
  status: 'pending' | 'dispatched' | 'processed' | 'failed';
}
