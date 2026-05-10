import type { AuthUser } from "./api/types";

const routePermissions: Array<{ prefix: string; permissions: string[] }> = [
  { prefix: "/reservations", permissions: ["reservations.manage", "tables.assign"] },
  { prefix: "/tables", permissions: ["reservations.manage", "tables.assign"] },
  { prefix: "/orders", permissions: ["orders.create", "orders.edit", "orders.send"] },
  { prefix: "/kitchen", permissions: ["kitchen.manage"] },
  { prefix: "/billing", permissions: ["billing.manage", "payments.process"] },
  { prefix: "/menu", permissions: ["menu.manage"] },
  { prefix: "/inventory", permissions: ["inventory.manage"] },
  { prefix: "/reports", permissions: ["reports.view"] },
  { prefix: "/staff", permissions: ["staff.manage"] },
  { prefix: "/audit", permissions: ["audit.view"] },
  { prefix: "/settings", permissions: ["settings.view", "settings.manage"] },
  { prefix: "/", permissions: ["dashboard.view"] },
];

export function hasPermission(user: AuthUser | null, permission: string) {
  if (!user) return false;
  return user.permissions.includes("all") || user.permissions.includes(permission);
}

export function hasAnyPermission(user: AuthUser | null, permissions: string[]) {
  return permissions.some((permission) => hasPermission(user, permission));
}

export function canAccessPath(user: AuthUser | null, pathname: string) {
  const rule = routePermissions.find((entry) => pathname === entry.prefix || pathname.startsWith(`${entry.prefix}/`));
  if (!rule) return true;
  return hasAnyPermission(user, rule.permissions);
}
