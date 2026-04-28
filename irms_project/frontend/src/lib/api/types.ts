export interface ApiEnvelope<T> {
  data: T;
  meta: {
    correlationId: string;
    timestamp: string;
  };
}

export interface ApiFieldError {
  field: string;
  message: string;
}

export interface AuthUser {
  userId: string;
  email: string;
  displayName: string;
  roles: string[];
  permissions: string[];
  sessionId: string;
}

export interface LoginResponse {
  token: string;
  expiresAt: string;
  user: AuthUser;
}

export interface OperationsReportResponse {
  generatedAt: string;
  periodStart: string;
  periodEnd: string;
  activeTables: number;
  totalTables: number;
  openOrders: number;
  readyToServe: number;
  kitchenQueue: number;
  averageKitchenWaitMinutes: number;
  averageServiceTimeMinutes: number;
  openLowStockAlerts: number;
  revenueToday: number;
  transactionsToday: number;
  reservationCountToday: number;
  refundsToday: number;
  revenueTrend: Array<{
    hour: string;
    revenue: number;
  }>;
  weeklyRevenue: Array<{
    day: string;
    revenue: number;
  }>;
  topDishes: Array<{
    name: string;
    orders: number;
    revenue: number;
  }>;
  peakHours: Array<{
    hour: string;
    orders: number;
  }>;
  categoryRevenue: Array<{
    name: string;
    value: number;
  }>;
  kitchenMetrics: Array<{
    station: string;
    avgTime: string;
    tickets: number;
    delayed: number;
  }>;
  outboxEventId?: string | null;
}

export interface DashboardMetricResponse {
  code: string;
  label: string;
  displayValue: string;
  valueType: string;
}

export interface DashboardKpiResponse {
  code: string;
  label: string;
  displayValue: string;
  unit: string;
  trend: string;
}

export interface DashboardResponse {
  operations: OperationsReportResponse;
  metrics: DashboardMetricResponse[];
  kpis: DashboardKpiResponse[];
  alerts: Array<{
    id: string;
    type: string;
    message: string;
    time: string;
  }>;
  activeOrders: Array<{
    id: string;
    table: number;
    server: string;
    items: number;
    status: string;
    time: string;
  }>;
}

export interface ReservationOverviewResponse {
  tables: Array<{
    id: string;
    number: number;
    capacity: number;
    status: string;
    notes: string;
    sessionId?: string | null;
    checkinTime?: string | null;
    reservationTime?: string | null;
  }>;
  reservations: Array<{
    id: string;
    guest: string;
    phone: string;
    email?: string | null;
    party: number;
    date: string;
    time: string;
    status: string;
    table?: number | null;
    notes?: string | null;
  }>;
  waitlist: Array<{
    id: string;
    name: string;
    phone: string;
    email?: string | null;
    party: number;
    estimatedWait: string;
    status: string;
    holdExpiresAt?: string | null;
    notes?: string | null;
    tablesAvailableNow?: number;
    suggestedTable?: number | null;
  }>;
}

export interface ReservationRecommendationResponse {
  date: string;
  time: string;
  party: number;
  candidateTables: Array<{
    id: string;
    number: number;
    capacity: number;
  }>;
  waitlistRecommended: boolean;
  estimatedWaitMinutes: number;
}

export interface MenuComboOptionResponse {
  id: string;
  menuItemId: string;
  menuItemName: string;
  station: string;
  extraPrice: number;
  active: boolean;
}

export interface MenuComboGroupResponse {
  id: string;
  name: string;
  minSelections: number;
  maxSelections: number;
  required: boolean;
  options: MenuComboOptionResponse[];
}

export interface MenuComboResponse {
  id: string;
  name: string;
  description: string;
  price: number;
  active: boolean;
  groups: MenuComboGroupResponse[];
}

export interface OrdersOverviewResponse {
  sessions: Array<{
    sessionId: string;
    tableNumber: number;
    guests: number;
  }>;
  menuItems: Array<{
    id: string;
    name: string;
    price: number;
    category: string;
    station: string;
    available: boolean;
    allergens: string[];
    modifierGroups: Array<{
      id: string;
      name: string;
      minSelect: number;
      maxSelect: number;
      required: boolean;
      multiSelect: boolean;
      options: Array<{
        id: string;
        name: string;
        extraPrice: number;
        active: boolean;
      }>;
    }>;
  }>;
  combos: MenuComboResponse[];
  orderedItems: Array<{
    id: string;
    orderId: string;
    orderStatus: string;
    name: string;
    quantity: number;
    price: number;
    note: string;
    status: string;
    station: string;
  }>;
  selectedSessionId?: string | null;
}

export interface KitchenOverviewResponse {
  stations: string[];
  tickets: Array<{
    id: string;
    orderId: string;
    tableNumber: number;
    serverName: string;
    priority: string;
    status: string;
    createdAt: string;
    items: Array<{
      id: string;
      orderItemId: string;
      name: string;
      quantity: number;
      modifiers: string[];
      allergyNotes?: string | null;
      specialInstructions?: string | null;
      status: string;
      station: string;
      holdReason?: string | null;
    }>;
  }>;
}

export interface BillingOverviewResponse {
  currentBill: {
    id: string;
    tableSessionId: string;
    tableNumber: number;
    items: Array<{
      lineId: string;
      name: string;
      quantity: number;
      total: number;
      modifiers?: string;
    }>;
    subtotal: number;
    taxAmount: number;
    taxRate: number;
    serviceFee: number;
    discount: number;
    tipAmount: number;
    total: number;
    status: string;
    splits: Array<{
      id: string;
      label: string;
      amount: number;
      tipAmount: number;
      status: string;
    }>;
    payments: Array<{
      id: string;
      billId: string;
      splitId?: string | null;
      splitLabel?: string | null;
      method: string;
      amount: number;
      status: string;
      paidAt: string;
    }>;
  } | null;
  billableSessions: Array<{
    sessionId: string;
    tableNumber: number;
    guests: number;
    openedAt: string;
  }>;
  recentBills: Array<{
    id: string;
    tableNumber: number;
    total: number;
    status: string;
    method: string;
    time: string;
  }>;
  refundQueue: Array<{
    id: string;
    paymentId: string;
    billId: string;
    amount: number;
    reason: string;
    status: string;
    requestedBy: string;
    approvedBy?: string | null;
    paymentMethod: string;
    tableNumber: number;
    createdAt: string;
  }>;
}

export interface MenuOverviewResponse {
  categories: Array<{
    id: string;
    name: string;
    items: number;
    active: boolean;
  }>;
  items: Array<{
    id: string;
    name: string;
    description: string;
    category: string;
    price: number;
    station: string;
    available: boolean;
    allergens: string[];
    modifiers: number;
    version: number;
    ingredients: string[];
  }>;
  promotions: Array<{
    id: string;
    code: string;
    discount: string;
    validUntil?: string | null;
    active: boolean;
  }>;
  combos: MenuComboResponse[];
}

export interface InventoryOverviewResponse {
  items: Array<{
    id: string;
    name: string;
    unit: string;
    current: number;
    min: number;
    max: number;
    cost: number;
    category: string;
    lastRestock: string;
    affected: string[];
  }>;
  transactions: Array<{
    id: string;
    item: string;
    type: string;
    quantity: number;
    by: string;
    time: string;
  }>;
  recommendations: Array<{
    inventoryItemId: string;
    name: string;
    threshold: number;
    targetQty: number;
    onHand: number;
    averageUsage: number;
    projectedNeed: number;
    recommendedOrderQty: number;
    confidence: string;
  }>;
  alerts: Array<{
    alertId: string;
    inventoryItemId: string;
    itemName: string;
    severity: string;
    status: string;
    createdAt: string;
    acknowledgedAt?: string | null;
    acknowledgedBy?: string | null;
    affectedDishes: string[];
  }>;
}

export interface ReportMetricResponse {
  key: string;
  label: string;
  valueType: string;
  displayValue: string;
}

export interface PeakHourRowResponse {
  businessDate: string;
  hourOfDay: number;
  orderCount: number;
  revenueTotal: number;
}

export interface SalesRowResponse {
  businessDate: string;
  orderCount: number;
  billCount: number;
  grossSales: number;
  refundTotal: number;
  netSales: number;
}

export interface BestSellingItemRowResponse {
  businessDate: string;
  menuItemId: string;
  itemName: string;
  quantitySold: number;
  revenueTotal: number;
}

export interface RevenueRowResponse {
  businessDate: string;
  paymentMethod: string;
  grossRevenue: number;
  discounts: number;
  refunds: number;
  netRevenue: number;
}

export interface KitchenBottleneckRowResponse {
  businessDate: string;
  station: string;
  delayedItemCount: number;
  averageDelayMinutes: number;
}

export interface ComboSalesRowResponse {
  businessDate: string;
  comboId: string;
  comboName: string;
  quantitySold: number;
  revenueTotal: number;
}

export interface SalesReportResponse {
  type?: string;
  periodStart: string;
  periodEnd: string;
  generatedAt: string;
  rows: SalesRowResponse[];
  metrics: ReportMetricResponse[];
}

export interface PeakHourReportResponse {
  type?: string;
  periodStart: string;
  periodEnd: string;
  generatedAt: string;
  rows: PeakHourRowResponse[];
  metrics: ReportMetricResponse[];
}

export interface BestSellingItemReportResponse {
  type?: string;
  periodStart: string;
  periodEnd: string;
  generatedAt: string;
  rows: BestSellingItemRowResponse[];
  metrics: ReportMetricResponse[];
}

export interface RevenueReportResponse {
  type?: string;
  periodStart: string;
  periodEnd: string;
  generatedAt: string;
  rows: RevenueRowResponse[];
  metrics: ReportMetricResponse[];
}

export interface KitchenBottleneckReportResponse {
  type?: string;
  periodStart: string;
  periodEnd: string;
  generatedAt: string;
  rows: KitchenBottleneckRowResponse[];
  metrics: ReportMetricResponse[];
}

export interface ComboSalesReportResponse {
  type?: string;
  periodStart: string;
  periodEnd: string;
  generatedAt: string;
  rows: ComboSalesRowResponse[];
  metrics: ReportMetricResponse[];
}

export interface StaffResponse {
  staff: Array<{
    userId: string;
    displayName: string;
    email: string;
    status: string;
    hireDate: string;
    roles: string[];
    permissions: string[];
  }>;
  roles: Array<{
    roleId: string;
    name: string;
    scope: string;
    description: string;
    permissions: string[];
  }>;
  shifts: Array<{
    shiftAssignmentId: string;
    userId: string;
    displayName: string;
    roleName: string;
    shiftDate: string;
    startAt: string;
    endAt: string;
    position: string;
    zone?: string | null;
    status: string;
  }>;
}

export interface AuditLogResponse {
  auditLogId: string;
  recordedAt: string;
  actorUserId: string;
  actorName: string;
  roles: string[];
  action: string;
  entityType: string;
  entityId: string;
  correlationId: string;
  reason?: string | null;
  followUp: boolean;
  ipAddress?: string | null;
  beforePayload: Record<string, unknown>;
  afterPayload: Record<string, unknown>;
}

export interface SettingsResponse {
  general: Record<string, unknown>;
  operations: Record<string, unknown>;
  sessionSecurity: Record<string, unknown>;
}

export interface NotificationInboxItem {
  id: string;
  type: string;
  title: string;
  body: string;
  priority: string;
  status: string;
  createdAt: string;
  readAt?: string | null;
  payload: Record<string, unknown>;
}
