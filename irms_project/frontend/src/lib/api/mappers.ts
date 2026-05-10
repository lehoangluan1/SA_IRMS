import type {
  AuditLogResponse,
  BillingOverviewResponse,
  DashboardResponse,
  InventoryOverviewResponse,
  KitchenOverviewResponse,
  MenuOverviewResponse,
  OperationsReportResponse,
  OrdersOverviewResponse,
  ReservationOverviewResponse,
  SettingsResponse,
  StaffResponse,
} from "./types";

export function mapDashboardData(response: DashboardResponse) {
  return {
    operations: response.operations,
    metrics: response.metrics,
    kpis: response.kpis,
    alerts: response.alerts,
    activeOrders: response.activeOrders,
  };
}

export function mapReservationData(response: ReservationOverviewResponse) {
  return {
    tables: response.tables,
    reservations: response.reservations.map((reservation) => ({
      ...reservation,
      status: reservation.status === "seated" ? "checked_in" : reservation.status,
    })),
    waitlist: response.waitlist.map((entry) => ({
      ...entry,
      wait: entry.estimatedWait,
    })),
  };
}

export function mapOrdersData(response: OrdersOverviewResponse) {
  return {
    ...response,
    orderedItems: response.orderedItems.map((item) => ({
      ...item,
      status: item.status === "hold_for_service" ? "delayed" : item.status,
    })),
  };
}

export function mapKitchenData(response: KitchenOverviewResponse) {
  return response;
}

export function mapBillingData(response: BillingOverviewResponse) {
  return response;
}

export function mapMenuData(response: MenuOverviewResponse) {
  return response;
}

export function mapInventoryData(response: InventoryOverviewResponse) {
  return response;
}

export function mapReportData(response: OperationsReportResponse) {
  return response;
}

export function mapStaffData(response: StaffResponse) {
  return response;
}

export function mapAuditData(response: AuditLogResponse[]) {
  return response;
}

export function mapSettingsData(response: SettingsResponse) {
  return response;
}
