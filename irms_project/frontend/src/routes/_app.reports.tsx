import { createFileRoute } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { Clock, Download, Layers3, PieChart as PieChartIcon, TrendingUp } from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  Pie,
  PieChart,
  XAxis,
  YAxis,
} from "recharts";
import { SectionHeader } from "@/components/shared/SectionHeader";
import {
  ChartContainer,
  ChartLegend,
  ChartLegendContent,
  ChartTooltip,
  ChartTooltipContent,
} from "@/components/ui/chart";
import { toast } from "sonner";
import { apiFetch, getStoredToken } from "@/lib/api/client";
import { buildApiUrl, ENDPOINTS } from "@/lib/api/endpoints";
import { mapReportData } from "@/lib/api/mappers";
import type {
  BestSellingItemReportResponse,
  ComboSalesReportResponse,
  KitchenBottleneckReportResponse,
  OperationsReportResponse,
  PeakHourReportResponse,
  RevenueReportResponse,
  SalesReportResponse,
} from "@/lib/api/types";

export const Route = createFileRoute("/_app/reports")({
  component: ReportsPage,
  head: () => ({ meta: [{ title: "Reports - IRMS" }] }),
});

const weeklyRevenueConfig = {
  netSales: {
    label: "Net revenue",
    color: "oklch(0.67 0.16 184)",
  },
};

const peakHoursConfig = {
  orderCount: {
    label: "Orders",
    color: "oklch(0.74 0.18 77)",
  },
};

const topDishesConfig = {
  quantitySold: {
    label: "Orders",
    color: "oklch(0.58 0.22 256)",
  },
};

const categoryColors = [
  "oklch(0.67 0.16 184)",
  "oklch(0.68 0.17 142)",
  "oklch(0.79 0.16 77)",
  "oklch(0.63 0.17 245)",
  "oklch(0.63 0.22 25)",
];

function ReportsPage() {
  const { data: operationsData } = useQuery({
    queryKey: ["reports", "operations"],
    queryFn: () => apiFetch<OperationsReportResponse>(ENDPOINTS.reports.operations),
  });

  const { data: salesData } = useQuery({
    queryKey: ["reports", "sales"],
    queryFn: () => apiFetch<SalesReportResponse>(ENDPOINTS.reports.sales),
  });

  const { data: peakHourData } = useQuery({
    queryKey: ["reports", "peak-hours"],
    queryFn: () => apiFetch<PeakHourReportResponse>(ENDPOINTS.reports.peakHours),
  });

  const { data: bestSellingData } = useQuery({
    queryKey: ["reports", "best-selling-items"],
    queryFn: () => apiFetch<BestSellingItemReportResponse>(ENDPOINTS.reports.bestSellingItems),
  });

  const { data: revenueData } = useQuery({
    queryKey: ["reports", "revenue"],
    queryFn: () => apiFetch<RevenueReportResponse>(ENDPOINTS.reports.revenue),
  });

  const { data: kitchenData } = useQuery({
    queryKey: ["reports", "kitchen-bottlenecks"],
    queryFn: () => apiFetch<KitchenBottleneckReportResponse>(ENDPOINTS.reports.kitchenBottlenecks),
  });

  const { data: comboSalesData } = useQuery({
    queryKey: ["reports", "combo-sales"],
    queryFn: () => apiFetch<ComboSalesReportResponse>(ENDPOINTS.reports.comboSales),
  });

  const operations = operationsData ? mapReportData(operationsData) : null;
  const weeklyRevenue = [...(salesData?.rows ?? [])]
    .sort((left, right) => normalizeDateValue(left.businessDate).localeCompare(normalizeDateValue(right.businessDate)))
    .slice(-7)
    .map((row) => ({
      label: formatWeekday(row.businessDate),
      netSales: Number(row.netSales),
      orders: Number(row.orderCount),
    }));
  const categoryRevenue = (operations?.categoryRevenue ?? []).map((slice, index) => ({
    ...slice,
    fill: categoryColors[index % categoryColors.length],
  }));
  const peakHours = [...(peakHourData?.rows ?? [])]
    .sort((left, right) => left.hourOfDay - right.hourOfDay)
    .map((row) => ({
      label: `${String(row.hourOfDay).padStart(2, "0")}:00`,
      orderCount: Number(row.orderCount),
      revenueTotal: Number(row.revenueTotal),
    }));
  const topDishes = [...(bestSellingData?.rows ?? [])]
    .sort((left, right) => Number(right.quantitySold) - Number(left.quantitySold))
    .slice(0, 5)
    .map((row) => ({
      name: row.itemName,
      quantitySold: Number(row.quantitySold),
      revenueTotal: Number(row.revenueTotal),
    }));
  const kitchenRows = [...(kitchenData?.rows ?? [])]
    .sort((left, right) => Number(right.delayedItemCount) - Number(left.delayedItemCount));
  const revenueRows = [...(revenueData?.rows ?? [])]
    .sort((left, right) => Number(right.netRevenue) - Number(left.netRevenue));
  const comboRows = [...(comboSalesData?.rows ?? [])]
    .sort((left, right) => Number(right.quantitySold) - Number(left.quantitySold));
  const generatedAt = operations?.generatedAt ? new Date(operations.generatedAt).toLocaleString() : "Updating...";

  return (
    <div className="space-y-6">
      <SectionHeader
        title="Reports"
        description="Review sales trends, category mix, busy hours, and kitchen performance."
        actions={(
          <>
            <span className="text-[10px] text-muted-foreground bg-secondary px-2.5 py-1 rounded-lg">
              Updated: {generatedAt}
            </span>
            <ExportLink type="operations" label="Operations CSV" />
            <ExportLink type="sales" label="Sales CSV" />
            <ExportLink type="best-selling-items" label="Top Dishes CSV" />
            <ExportLink type="combo-sales" label="Combo Sales CSV" />
          </>
        )}
      />

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <Panel
          className="xl:col-span-2"
          title="Weekly Revenue"
          description="Net sales over the latest seven business days."
          actionLabel={`${weeklyRevenue.length} days`}
        >
          {weeklyRevenue.length ? (
            <ChartContainer config={weeklyRevenueConfig} className="h-[260px] w-full aspect-auto">
              <BarChart data={weeklyRevenue}>
                <CartesianGrid vertical={false} strokeDasharray="4 4" />
                <XAxis dataKey="label" axisLine={false} tickLine={false} />
                <YAxis axisLine={false} tickLine={false} tickFormatter={(value) => `$${value}`} />
                <ChartTooltip content={<ChartTooltipContent />} />
                <Bar dataKey="netSales" fill="var(--color-netSales)" radius={[8, 8, 0, 0]} />
              </BarChart>
            </ChartContainer>
          ) : (
            <EmptyPanel message="No weekly revenue is available yet." />
          )}
        </Panel>

        <Panel
          title="Revenue by Category"
          description="How each menu category contributes to sales."
          actionLabel={`${categoryRevenue.length} categories`}
        >
          {categoryRevenue.length ? (
            <ChartContainer
              config={Object.fromEntries(categoryRevenue.map((slice) => [slice.name, { label: slice.name, color: slice.fill }]))}
              className="h-[260px] w-full aspect-auto"
            >
              <PieChart>
                <ChartTooltip content={<ChartTooltipContent nameKey="name" />} />
                <Pie
                  data={categoryRevenue}
                  dataKey="value"
                  nameKey="name"
                  innerRadius={58}
                  outerRadius={92}
                  paddingAngle={4}
                >
                  {categoryRevenue.map((slice) => (
                    <Cell key={slice.name} fill={slice.fill} />
                  ))}
                </Pie>
                <ChartLegend content={<ChartLegendContent nameKey="name" />} />
              </PieChart>
            </ChartContainer>
          ) : (
            <EmptyPanel message="Category mix is not available yet." />
          )}
        </Panel>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <Panel
          title="Peak Hours (Orders / Hour)"
          description="When guests place the most orders."
          actionLabel={`${peakHours.length} slots`}
        >
          {peakHours.length ? (
            <ChartContainer config={peakHoursConfig} className="h-[240px] w-full aspect-auto">
              <LineChart data={peakHours}>
                <CartesianGrid vertical={false} strokeDasharray="4 4" />
                <XAxis dataKey="label" axisLine={false} tickLine={false} />
                <YAxis axisLine={false} tickLine={false} />
                <ChartTooltip content={<ChartTooltipContent />} />
                <Line
                  type="monotone"
                  dataKey="orderCount"
                  stroke="var(--color-orderCount)"
                  strokeWidth={2.5}
                  dot={{ r: 3 }}
                  activeDot={{ r: 4 }}
                />
              </LineChart>
            </ChartContainer>
          ) : (
            <EmptyPanel message="No peak-hour information is available yet." />
          )}
        </Panel>

        <Panel
          title="Top Dishes"
          description="Best-selling items ranked by quantity sold."
          actionLabel={`${topDishes.length} dishes`}
        >
          {topDishes.length ? (
            <ChartContainer config={topDishesConfig} className="h-[240px] w-full aspect-auto">
              <BarChart data={topDishes} layout="vertical" margin={{ left: 24 }}>
                <CartesianGrid horizontal={false} strokeDasharray="4 4" />
                <XAxis type="number" axisLine={false} tickLine={false} />
                <YAxis type="category" dataKey="name" axisLine={false} tickLine={false} width={110} />
                <ChartTooltip content={<ChartTooltipContent />} />
                <Bar dataKey="quantitySold" fill="var(--color-quantitySold)" radius={[0, 8, 8, 0]} />
              </BarChart>
            </ChartContainer>
          ) : (
            <EmptyPanel message="No best-selling items are available yet." />
          )}
        </Panel>
      </div>

      <Panel
        title="Kitchen Station Performance"
        description="Compare kitchen stations by wait time and delayed items."
        actionLabel={`${kitchenRows.length} stations`}
      >
        {kitchenRows.length ? (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  {["Station", "Avg Delay", "Delayed Items", "Performance"].map((header) => (
                    <th key={header} className="text-left py-2.5 px-3 text-xs font-medium text-muted-foreground uppercase tracking-wider">
                      {header}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {kitchenRows.map((row) => {
                  const performance = performanceScore(Number(row.averageDelayMinutes), Number(row.delayedItemCount));
                  return (
                    <tr key={`${row.businessDate}-${row.station}`} className="border-b border-border/30">
                      <td className="py-3 px-3 text-foreground capitalize">{row.station}</td>
                      <td className="py-3 px-3 text-muted-foreground">{row.averageDelayMinutes} min</td>
                      <td className="py-3 px-3 text-foreground">{row.delayedItemCount}</td>
                      <td className="py-3 px-3">
                        <div className="flex items-center gap-3">
                          <div className="h-2 w-28 rounded-full bg-muted">
                            <div
                              className={`h-2 rounded-full ${performance >= 80 ? "bg-emerald-500" : performance >= 60 ? "bg-amber-500" : "bg-red-500"}`}
                              style={{ width: `${performance}%` }}
                            />
                          </div>
                          <span className="text-xs text-muted-foreground">{performance}%</span>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyPanel message="No kitchen station information is available yet." />
        )}
      </Panel>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <Panel
          title="Revenue Mix by Payment Method"
          description="Payment totals by method for cashier review."
          actionLabel={`${revenueRows.length} payment methods`}
        >
          <ReportTable
            headers={["Method", "Gross", "Refunds", "Net"]}
            rows={revenueRows.map((row) => [
              row.paymentMethod.replace("_", " "),
              formatCurrency(row.grossRevenue),
              formatCurrency(row.refunds),
              formatCurrency(row.netRevenue),
            ])}
            emptyMessage="No payment totals are available yet."
          />
        </Panel>

        <Panel
          title="Combo Sales Detail"
          description="Combo item sales and revenue."
          actionLabel={`${comboRows.length} combos`}
        >
          <ReportTable
            headers={["Combo", "Quantity Sold", "Revenue", "Business Date"]}
            rows={comboRows.map((row) => [
              row.comboName,
              String(row.quantitySold),
              formatCurrency(row.revenueTotal),
              normalizeDateValue(row.businessDate),
            ])}
            emptyMessage="No combo sales are available yet."
          />
        </Panel>
      </div>
    </div>
  );
}

function Panel({
  title,
  description,
  actionLabel,
  className,
  children,
}: {
  title: string;
  description: string;
  actionLabel?: string;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <section className={`bg-card rounded-xl border border-border p-5 shadow-sm ${className ?? ""}`.trim()}>
      <div className="flex items-start justify-between gap-4 mb-4">
        <div>
          <h2 className="text-sm font-semibold text-foreground">{title}</h2>
          <p className="text-xs text-muted-foreground mt-1">{description}</p>
        </div>
        {actionLabel ? <span className="text-xs text-muted-foreground">{actionLabel}</span> : null}
      </div>
      {children}
    </section>
  );
}

function ExportLink({ type, label }: { type: string; label: string }) {
  return (
    <button
      type="button"
      onClick={() => void downloadReportCsv(type, label)}
      className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-secondary text-xs text-muted-foreground hover:text-foreground"
    >
      <Download className="w-3.5 h-3.5" />
      {label}
    </button>
  );
}

async function downloadReportCsv(type: string, label: string) {
  const token = getStoredToken();
  const headers = new Headers({
    Accept: "text/csv",
    "X-Correlation-Id": createCorrelationId(),
  });
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const params = new URLSearchParams({ type, format: "csv" });
  const response = await fetch(buildApiUrl(`${ENDPOINTS.reports.export}?${params.toString()}`), { headers });
  if (!response.ok) {
    toast.error(await readDownloadError(response));
    return;
  }

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${type}-report.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
  toast.success(`${label} exported`);
}

async function readDownloadError(response: Response) {
  const fallback = `Report export failed with status ${response.status}.`;
  try {
    const payload = await response.json();
    return payload?.message ?? fallback;
  } catch {
    return fallback;
  }
}

function createCorrelationId() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `corr-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function ReportTable({
  headers,
  rows,
  emptyMessage,
}: {
  headers: string[];
  rows: string[][];
  emptyMessage: string;
}) {
  if (rows.length === 0) {
    return <EmptyPanel message={emptyMessage} compact />;
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-border">
            {headers.map((header) => (
              <th key={header} className="text-left py-2.5 px-3 text-xs font-medium text-muted-foreground uppercase tracking-wider">
                {header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={`${row.join("-")}-${index}`} className="border-b border-border/30">
              {row.map((value, cellIndex) => (
                <td key={`${value}-${cellIndex}`} className="py-2.5 px-3 text-xs text-foreground">
                  {value}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
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

function normalizeDateValue(value: unknown) {
  if (typeof value === "string") {
    return value;
  }
  if (Array.isArray(value) && value.length >= 3) {
    const [year, month, day] = value;
    return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
  }
  if (value && typeof value === "object") {
    const date = value as { year?: number; month?: number; day?: number; monthValue?: number; dayOfMonth?: number };
    if (date.year && (date.monthValue ?? date.month) && (date.dayOfMonth ?? date.day)) {
      return `${date.year}-${String(date.monthValue ?? date.month).padStart(2, "0")}-${String(date.dayOfMonth ?? date.day).padStart(2, "0")}`;
    }
  }
  return "";
}

function formatWeekday(value: unknown) {
  const normalized = normalizeDateValue(value);
  return normalized ? new Date(`${normalized}T00:00:00`).toLocaleDateString("en-US", { weekday: "short" }) : "";
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 2,
  }).format(value ?? 0);
}

function performanceScore(averageDelayMinutes: number, delayedItemCount: number) {
  const score = 100 - averageDelayMinutes * 3 - delayedItemCount * 8;
  return Math.max(18, Math.min(100, Math.round(score)));
}
