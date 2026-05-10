import { createFileRoute } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import {
  AlertTriangle,
  CalendarDays,
  ClipboardList,
  Clock,
  DollarSign,
  Flame,
  Receipt,
  Users,
} from "lucide-react";
import { CartesianGrid, Line, LineChart, XAxis, YAxis } from "recharts";
import { SectionHeader } from "@/components/shared/SectionHeader";
import { StatCard } from "@/components/shared/StatCard";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { ChartContainer, ChartTooltip, ChartTooltipContent } from "@/components/ui/chart";
import { apiFetch } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import { mapDashboardData } from "@/lib/api/mappers";
import type { DashboardResponse } from "@/lib/api/types";

export const Route = createFileRoute("/_app/")({
  component: DashboardPage,
  head: () => ({
    meta: [
      { title: "Dashboard - IRMS" },
      { name: "description", content: "Real-time restaurant operations overview" },
    ],
  }),
});

const statusVariant: Record<string, "success" | "info" | "warning"> = {
  ready: "success",
  confirmed: "info",
  in_progress: "warning",
};

const revenueChartConfig = {
  revenue: {
    label: "Revenue",
    color: "oklch(0.61 0.22 262)",
  },
};

export default function DashboardPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["dashboard"],
    queryFn: () => apiFetch<DashboardResponse>(ENDPOINTS.dashboard),
  });

  const mapped = data ? mapDashboardData(data) : null;
  const operations = mapped?.operations ?? null;
  const alerts = mapped?.alerts ?? [];
  const activeOrders = mapped?.activeOrders ?? [];
  const generatedAt = operations?.generatedAt ? new Date(operations.generatedAt) : null;
  const occupancy = operations?.totalTables
    ? Math.round((operations.activeTables / operations.totalTables) * 100)
    : 0;

  return (
    <div className="space-y-6">
      <SectionHeader
        title="Today at a Glance"
        description={`Live floor activity, kitchen pace, and open alerts. ${
          isLoading
            ? "Refreshing latest activity..."
            : generatedAt
              ? `Updated ${generatedAt.toLocaleString()}.`
              : "Update time unavailable."
        }`}
      />

      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard
          title="Active Tables"
          value={`${operations?.activeTables ?? 0} / ${operations?.totalTables ?? 0}`}
          subtitle={`${occupancy}% occupancy`}
          icon={Users}
        />
        <StatCard
          title="Open Orders"
          value={operations?.openOrders ?? 0}
          subtitle={`${operations?.readyToServe ?? 0} ready to serve`}
          icon={ClipboardList}
          variant="warning"
        />
        <StatCard
          title="Kitchen Queue"
          value={operations?.kitchenQueue ?? 0}
          subtitle={`Avg wait ${operations?.averageKitchenWaitMinutes ?? 0} min`}
          icon={Flame}
          variant="danger"
        />
        <StatCard
          title="Revenue Today"
          value={formatCurrency(operations?.revenueToday ?? 0)}
          subtitle={`${operations?.transactionsToday ?? 0} transactions`}
          icon={DollarSign}
          variant="success"
        />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <section className="xl:col-span-2 bg-card rounded-xl border border-border p-5 shadow-sm">
          <div className="flex items-center justify-between gap-3 mb-4">
            <div>
              <h2 className="text-sm font-semibold text-foreground">Revenue Trend - Today</h2>
              <p className="text-xs text-muted-foreground mt-1">
                Payments collected across today.
              </p>
            </div>
            <span className="text-xs text-emerald-600">
              {operations?.transactionsToday ?? 0} payments today
            </span>
          </div>
          {operations?.revenueTrend.length ? (
            <ChartContainer config={revenueChartConfig} className="h-[280px] w-full aspect-auto">
              <LineChart data={operations.revenueTrend}>
                <CartesianGrid vertical={false} strokeDasharray="4 4" />
                <XAxis dataKey="hour" axisLine={false} tickLine={false} />
                <YAxis axisLine={false} tickLine={false} tickFormatter={(value) => `$${value}`} />
                <ChartTooltip content={<ChartTooltipContent />} />
                <Line
                  type="monotone"
                  dataKey="revenue"
                  stroke="var(--color-revenue)"
                  strokeWidth={2.5}
                  dot={false}
                  activeDot={{ r: 4 }}
                />
              </LineChart>
            </ChartContainer>
          ) : (
            <EmptyPanel message="No payments have been collected yet today." />
          )}
        </section>

        <section className="bg-card rounded-xl border border-border p-5 shadow-sm">
          <div className="flex items-center justify-between gap-3 mb-4">
            <h2 className="text-sm font-semibold text-foreground">Alerts</h2>
            <span className="text-xs text-muted-foreground">{alerts.length} open</span>
          </div>
          <div className="space-y-2.5">
            {alerts.length === 0 ? (
              <EmptyPanel message="No active operational alerts." compact />
            ) : alerts.map((alert) => (
              <div
                key={alert.id}
                className={`rounded-lg border p-3 ${
                  alert.type === "danger"
                    ? "border-red-200 bg-red-50"
                    : alert.type === "warning"
                      ? "border-amber-200 bg-amber-50"
                      : "border-blue-200 bg-blue-50"
                }`}
              >
                <div className="flex items-start gap-2.5">
                  <AlertTriangle
                    className={`w-4 h-4 mt-0.5 shrink-0 ${
                      alert.type === "danger"
                        ? "text-red-500"
                        : alert.type === "warning"
                          ? "text-amber-500"
                          : "text-blue-500"
                    }`}
                  />
                  <div>
                    <p className="text-xs leading-snug text-foreground">{alert.message}</p>
                    <p className="text-[10px] text-muted-foreground mt-1">{alert.time}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>

      <section className="bg-card rounded-xl border border-border p-5 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-semibold text-foreground">Active Orders</h2>
          <span className="text-xs text-muted-foreground">{activeOrders.length} live orders</span>
        </div>
        {activeOrders.length === 0 ? (
          <EmptyPanel message="No active orders are currently in service." compact />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  {["Order", "Table", "Server", "Items", "Status", "Elapsed"].map((header) => (
                    <th key={header} className="text-left py-2.5 px-3 text-xs font-medium text-muted-foreground uppercase tracking-wider">
                      {header}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {activeOrders.map((order) => (
                  <tr key={order.id} className="border-b border-border/40 hover:bg-muted/30 transition-colors">
                    <td className="py-3 px-3 font-mono text-sm text-foreground">{order.id}</td>
                    <td className="py-3 px-3 text-foreground">#{order.table}</td>
                    <td className="py-3 px-3 text-muted-foreground">{order.server}</td>
                    <td className="py-3 px-3 text-muted-foreground">{order.items}</td>
                    <td className="py-3 px-3">
                      <StatusBadge status={order.status.replace("_", " ")} variant={statusVariant[order.status] ?? "info"} />
                    </td>
                    <td className="py-3 px-3 text-muted-foreground">
                      <span className="flex items-center gap-1">
                        <Clock className="w-3 h-3" />
                        {order.time}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <StatCard
          title="Avg Service Time"
          value={`${operations?.averageServiceTimeMinutes ?? 0} min`}
          subtitle={`Kitchen wait baseline ${operations?.averageKitchenWaitMinutes ?? 0} min`}
          icon={Clock}
          variant="success"
        />
        <StatCard
          title="Reservations Today"
          value={operations?.reservationCountToday ?? 0}
          subtitle={`${operations?.openLowStockAlerts ?? 0} low-stock alerts still open`}
          icon={CalendarDays}
        />
        <StatCard
          title="Refunds Today"
          value={formatCurrency(operations?.refundsToday ?? 0)}
          subtitle={generatedAt ? `Updated ${generatedAt.toLocaleTimeString()}` : "Update pending"}
          icon={Receipt}
          variant="danger"
        />
      </div>
    </div>
  );
}

function EmptyPanel({ message, compact = false }: { message: string; compact?: boolean }) {
  return (
    <div className={`${compact ? "py-6" : "py-12"} text-center text-sm text-muted-foreground`}>
      {message}
    </div>
  );
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 0,
  }).format(value ?? 0);
}
