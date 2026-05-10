import { Link, useLocation } from "@tanstack/react-router";
import {
  LayoutDashboard,
  CalendarDays,
  ClipboardList,
  Flame,
  Receipt,
  BookOpen,
  Package,
  BarChart3,
  Users,
  Shield,
  Settings,
  ChevronLeft,
  ChevronRight,
  UtensilsCrossed,
} from "lucide-react";
import { useAuth } from "@/lib/auth";
import { hasAnyPermission } from "@/lib/permissions";

const navItems = [
  { label: "Dashboard", to: "/" as const, icon: LayoutDashboard, exact: true, permissions: ["dashboard.view"] },
  { label: "Reservations & Tables", to: "/reservations" as const, icon: CalendarDays, exact: false, permissions: ["reservations.manage", "tables.assign"] },
  { label: "Orders", to: "/orders" as const, icon: ClipboardList, exact: false, permissions: ["orders.create", "orders.edit", "orders.send"] },
  { label: "Kitchen", to: "/kitchen" as const, icon: Flame, exact: false, permissions: ["kitchen.manage"] },
  { label: "Billing", to: "/billing" as const, icon: Receipt, exact: false, permissions: ["billing.manage", "payments.process"] },
  { label: "Menu", to: "/menu" as const, icon: BookOpen, exact: false, permissions: ["menu.manage"] },
  { label: "Inventory", to: "/inventory" as const, icon: Package, exact: false, permissions: ["inventory.manage"] },
  { label: "Reports", to: "/reports" as const, icon: BarChart3, exact: false, permissions: ["reports.view"] },
  { label: "Staff & Roles", to: "/staff" as const, icon: Users, exact: false, permissions: ["staff.manage"] },
  { label: "Audit Logs", to: "/audit" as const, icon: Shield, exact: false, permissions: ["audit.view"] },
  { label: "Settings", to: "/settings" as const, icon: Settings, exact: false, permissions: ["settings.view", "settings.manage"] },
];

interface AppSidebarProps {
  collapsed: boolean;
  onToggle: () => void;
}

export function AppSidebar({ collapsed, onToggle }: AppSidebarProps) {
  const location = useLocation();
  const { user } = useAuth();
  const visibleNavItems = navItems.filter((item) => hasAnyPermission(user, item.permissions));

  return (
    <aside
      className={`hidden lg:flex flex-col h-full bg-sidebar border-r border-sidebar-border transition-all duration-300 shrink-0 ${
        collapsed ? "w-16" : "w-60"
      }`}
    >
      <div className="flex items-center h-14 px-4 border-b border-sidebar-border gap-2">
        <div className="w-7 h-7 rounded-lg bg-primary flex items-center justify-center shrink-0">
          <UtensilsCrossed className="w-4 h-4 text-primary-foreground" />
        </div>
        {!collapsed && (
          <span className="font-heading font-bold text-sm text-sidebar-foreground tracking-wide">
            IRMS
          </span>
        )}
      </div>

      <nav className="flex-1 py-3 overflow-y-auto">
        {visibleNavItems.map((item) => {
          const isActive = item.exact
            ? location.pathname === item.to
            : location.pathname.startsWith(item.to);
          return (
            <Link
              key={item.to}
              to={item.to}
              className={`flex items-center gap-3 px-3 py-2 mx-2 mb-0.5 rounded-lg text-[13px] transition-colors ${
                isActive
                  ? "bg-primary/10 text-primary font-medium"
                  : "text-muted-foreground hover:text-sidebar-foreground hover:bg-sidebar-accent"
              }`}
            >
              <item.icon className="w-[18px] h-[18px] shrink-0" />
              {!collapsed && <span className="truncate">{item.label}</span>}
            </Link>
          );
        })}
      </nav>

      <div className="p-3 border-t border-sidebar-border">
        <button
          onClick={onToggle}
          className="flex items-center justify-center w-full p-2 rounded-lg text-muted-foreground hover:text-sidebar-foreground hover:bg-sidebar-accent transition-colors"
        >
          {collapsed ? (
            <ChevronRight className="w-4 h-4" />
          ) : (
            <ChevronLeft className="w-4 h-4" />
          )}
        </button>
      </div>
    </aside>
  );
}
