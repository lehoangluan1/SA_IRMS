const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();

export const API_BASE_URL =
  configuredApiBaseUrl && configuredApiBaseUrl !== "/" ? configuredApiBaseUrl.replace(/\/$/, "") : "";

export function buildApiUrl(path: string) {
  return `${API_BASE_URL}${path}`;
}

export const ENDPOINTS = {
  auth: {
    login: "/api/auth/login",
    logout: "/api/auth/logout",
    me: "/api/auth/me",
  },
  dashboard: "/api/dashboard",
  reservations: {
    overview: "/api/reservations/overview",
    recommendations: "/api/reservations/recommendations",
    list: "/api/reservations",
    confirm: (id: string) => `/api/reservations/${id}/confirm`,
    checkIn: (id: string) => `/api/reservations/${id}/check-in`,
    noShow: (id: string) => `/api/reservations/${id}/no-show`,
    update: (id: string) => `/api/reservations/${id}`,
  },
  tables: {
    create: "/api/tables",
    update: (id: string) => `/api/tables/${id}`,
    delete: (id: string) => `/api/tables/${id}`,
    status: (id: string) => `/api/tables/${id}/status`,
  },
  waitlist: {
    create: "/api/waitlist",
    notify: (id: string) => `/api/waitlist/${id}/notify`,
    skip: (id: string) => `/api/waitlist/${id}/skip`,
    prioritize: (id: string) => `/api/waitlist/${id}/prioritize`,
    seat: (id: string) => `/api/waitlist/${id}/seat`,
  },
  notifications: "/api/notifications",
  notificationRead: (id: string) => `/api/notifications/${id}/read`,
  orders: {
    overview: "/api/orders/overview",
    create: "/api/orders",
    confirm: (id: string) => `/api/orders/${id}/confirm`,
    cancel: (id: string) => `/api/orders/${id}/cancel`,
    serveItem: (id: string) => `/api/orders/items/${id}/served`,
    delayItem: (id: string) => `/api/orders/items/${id}/delayed`,
    sendItem: (id: string) => `/api/orders/items/${id}/send`,
    cancelItem: (id: string) => `/api/orders/items/${id}/cancel`,
  },
  kitchen: {
    overview: "/api/kitchen/overview",
    ticketStatus: (id: string) => `/api/kitchen/tickets/${id}/status`,
    itemStatus: (id: string) => `/api/kitchen/items/${id}/status`,
    priority: (id: string) => `/api/kitchen/tickets/${id}/priority`,
    cancel: (id: string) => `/api/kitchen/tickets/${id}/cancel`,
    serve: (id: string) => `/api/kitchen/tickets/${id}/serve`,
    returnTicket: (id: string) => `/api/kitchen/tickets/${id}/return`,
  },
  billing: {
    overview: "/api/billing/overview",
    create: "/api/bills",
    update: (id: string) => `/api/bills/${id}`,
    splits: (id: string) => `/api/bills/${id}/splits`,
    payments: (id: string) => `/api/bills/${id}/payments`,
    promotions: (id: string) => `/api/bills/${id}/promotions`,
    receipt: (id: string) => `/api/payments/${id}/receipt`,
    receiptDocument: (id: string) => `/api/payments/${id}/receipt/document`,
    refunds: (id: string) => `/api/payments/${id}/refunds`,
    pendingRefunds: "/api/refunds/pending",
    refundApproval: (id: string) => `/api/refunds/${id}/approval`,
  },
  menu: {
    overview: "/api/menu",
    combos: "/api/menu/combos",
    combo: (id: string) => `/api/menu/combos/${id}`,
    categories: "/api/menu/categories",
    category: (id: string) => `/api/menu/categories/${id}`,
    items: "/api/menu/items",
    item: (id: string) => `/api/menu/items/${id}`,
    itemAvailability: (id: string) => `/api/menu/items/${id}/availability`,
    promotions: "/api/menu/promotions",
    promotion: (id: string) => `/api/menu/promotions/${id}`,
  },
  inventory: {
    overview: "/api/inventory",
    items: "/api/inventory/items",
    item: (id: string) => `/api/inventory/items/${id}`,
    alertAcknowledge: (id: string) => `/api/inventory/alerts/${id}/acknowledge`,
  },
  reports: {
    operations: "/api/reports/operations",
    sales: "/api/reports/sales",
    peakHours: "/api/reports/peak-hours",
    bestSellingItems: "/api/reports/best-selling-items",
    revenue: "/api/reports/revenue",
    kitchenBottlenecks: "/api/reports/kitchen-bottlenecks",
    staffEfficiency: "/api/reports/staff-efficiency",
    inventoryUsage: "/api/reports/inventory-usage",
    comboSales: "/api/reports/combo-sales",
    export: "/api/reports/export",
  },
  staff: {
    overview: "/api/staff",
    roles: (id: string) => `/api/staff/${id}/roles`,
  },
  shifts: "/api/shifts",
  audit: {
    logs: "/api/audit/logs",
    followUp: (id: string) => `/api/audit/logs/${id}/follow-up`,
    export: "/api/audit/export",
  },
  settings: "/api/settings",
  health: "/api/health",
} as const;
