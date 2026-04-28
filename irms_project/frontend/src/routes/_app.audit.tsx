import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useDeferredValue, useMemo, useState } from "react";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Shield, Search, Download, Eye, Flag } from "lucide-react";
import { apiFetch } from "@/lib/api/client";
import { buildApiUrl, ENDPOINTS } from "@/lib/api/endpoints";
import { mapAuditData } from "@/lib/api/mappers";
import type { AuditLogResponse } from "@/lib/api/types";
import { toast } from "sonner";

export const Route = createFileRoute("/_app/audit")({
  component: AuditPage,
  head: () => ({ meta: [{ title: "Audit & Investigation - IRMS" }] }),
});

const actionSeverity: Record<string, "danger" | "warning" | "info" | "neutral"> = {
  "billing.refund.issued": "danger",
  "kitchen.ticket.cancelled": "warning",
  "menu.item.updated": "warning",
  "inventory.manual_adjustment": "info",
  "iam.role.modified": "danger",
  "audit.log.exported": "info",
};

const actionOptions = [
  { label: "All Actions", value: "all" },
  { label: "Refunds", value: "billing.refund.issued" },
  { label: "Cancellations", value: "kitchen.ticket.cancelled" },
  { label: "Menu Updates", value: "menu.item.updated" },
  { label: "Role Changes", value: "iam.role.modified" },
  { label: "Inventory Adjustments", value: "inventory.manual_adjustment" },
];

const roleOptions = [
  { label: "All Roles", value: "all" },
  { label: "Admin", value: "admin" },
  { label: "Manager", value: "manager" },
  { label: "Chef", value: "chef" },
  { label: "Cashier", value: "cashier" },
  { label: "Server", value: "server" },
];

function AuditPage() {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState("");
  const [actionFilter, setActionFilter] = useState("all");
  const [roleFilter, setRoleFilter] = useState("all");
  const [datePreset, setDatePreset] = useState("last7");
  const [customStartDate, setCustomStartDate] = useState("");
  const [customEndDate, setCustomEndDate] = useState("");
  const [selectedLog, setSelectedLog] = useState<AuditLogResponse | null>(null);
  const deferredSearch = useDeferredValue(searchQuery);

  const { startDate, endDate } = resolveAuditDateRange(datePreset, customStartDate, customEndDate);

  const { data, isLoading } = useQuery({
    queryKey: ["audit", deferredSearch, actionFilter, roleFilter, startDate, endDate],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (deferredSearch.trim() !== "") params.set("search", deferredSearch.trim());
      if (actionFilter !== "all") params.set("action", actionFilter);
      if (roleFilter !== "all") params.set("actorRole", roleFilter);
      if (startDate) params.set("startDate", startDate);
      if (endDate) params.set("endDate", endDate);
      params.set("limit", "50");
      return apiFetch<AuditLogResponse[]>(`${ENDPOINTS.audit.logs}?${params.toString()}`);
    },
  });

  const toggleMutation = useMutation({
    mutationFn: (payload: { auditLogId: string; followUp: boolean }) =>
      apiFetch(ENDPOINTS.audit.followUp(payload.auditLogId), {
        method: "PATCH",
        body: JSON.stringify({ followUp: payload.followUp }),
      }),
    onSuccess: async (_, variables) => {
      toast.success(variables.followUp ? "Follow-up flag added" : "Follow-up flag removed");
      await queryClient.invalidateQueries({ queryKey: ["audit"] });
    },
    onError: (error: Error) => {
      toast.error(error.message);
    },
  });

  const auditLogs = data ? mapAuditData(data) : [];

  const exportHref = useMemo(() => {
    const params = new URLSearchParams();
    if (deferredSearch.trim() !== "") params.set("search", deferredSearch.trim());
    if (actionFilter !== "all") params.set("action", actionFilter);
    if (roleFilter !== "all") params.set("actorRole", roleFilter);
    if (startDate) params.set("startDate", startDate);
    if (endDate) params.set("endDate", endDate);
    params.set("format", "csv");
    return buildApiUrl(`${ENDPOINTS.audit.export}?${params.toString()}`);
  }, [actionFilter, deferredSearch, endDate, roleFilter, startDate]);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-heading font-bold text-foreground">Audit & Investigation</h1>
          <p className="text-xs text-muted-foreground mt-1">
            Immutable audit trail - Before and after values - Correlation ID tracing - Investigation-friendly
          </p>
        </div>
        <a
          href={exportHref}
          className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-secondary text-xs text-muted-foreground hover:text-foreground"
        >
          <Download className="w-3.5 h-3.5" /> Export
        </a>
      </div>

      <div className="bg-card rounded-xl border border-border p-4">
        <div className="flex flex-wrap gap-3">
          <div className="relative flex-1 min-w-48">
            <Search className="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <input
              className="w-full pl-8 pr-3 py-1.5 rounded-lg bg-input border border-border text-xs text-foreground focus:outline-none focus:ring-1 focus:ring-ring"
              placeholder="Search by actor, entity, action, or correlation ID..."
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
            />
          </div>
          <select
            className="px-3 py-1.5 rounded-lg bg-input border border-border text-xs text-foreground"
            value={actionFilter}
            onChange={(event) => setActionFilter(event.target.value)}
          >
            {actionOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <select
            className="px-3 py-1.5 rounded-lg bg-input border border-border text-xs text-foreground"
            value={roleFilter}
            onChange={(event) => setRoleFilter(event.target.value)}
          >
            {roleOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <select
            className="px-3 py-1.5 rounded-lg bg-input border border-border text-xs text-foreground"
            value={datePreset}
            onChange={(event) => setDatePreset(event.target.value)}
          >
            <option value="today">Today</option>
            <option value="last7">Last 7 days</option>
            <option value="last30">Last 30 days</option>
            <option value="all">All time</option>
            <option value="custom">Custom range</option>
          </select>
          {datePreset === "custom" && (
            <>
              <input
                type="date"
                className="px-3 py-1.5 rounded-lg bg-input border border-border text-xs text-foreground"
                value={customStartDate}
                onChange={(event) => setCustomStartDate(event.target.value)}
              />
              <input
                type="date"
                className="px-3 py-1.5 rounded-lg bg-input border border-border text-xs text-foreground"
                value={customEndDate}
                onChange={(event) => setCustomEndDate(event.target.value)}
              />
            </>
          )}
        </div>
      </div>

      <div className="bg-card rounded-xl border border-border p-5">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-medium text-foreground flex items-center gap-2">
            <Shield className="w-4 h-4 text-primary" /> Audit Log
          </h2>
          <span className="text-xs text-muted-foreground">{auditLogs.length} entries shown</span>
        </div>

        {isLoading ? (
          <div className="py-10 text-center text-sm text-muted-foreground">Loading audit records...</div>
        ) : auditLogs.length === 0 ? (
          <div className="py-10 text-center text-sm text-muted-foreground">No audit entries match the selected filters.</div>
        ) : (
          <div className="space-y-3">
            {auditLogs.map((log) => (
              <div
                key={log.auditLogId}
                className={`p-4 rounded-xl border ${log.followUp ? "border-warning/40 bg-warning/5" : "border-border bg-secondary/20"}`}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-mono text-xs text-foreground">{log.auditLogId}</span>
                    <StatusBadge
                      status={log.action.split(".").slice(-1)[0]}
                      variant={actionSeverity[log.action] ?? "info"}
                    />
                    {log.followUp && (
                      <span className="flex items-center gap-0.5 px-1.5 py-0.5 rounded text-[9px] bg-warning/20 text-warning font-medium">
                        <Flag className="w-2.5 h-2.5" /> Follow-up
                      </span>
                    )}
                  </div>
                  <span className="text-[10px] font-mono text-muted-foreground">{log.recordedAt}</span>
                </div>

                <div className="mt-2 grid grid-cols-2 md:grid-cols-4 gap-2 text-[10px]">
                  <div>
                    <span className="text-muted-foreground">Actor:</span>
                    <span className="text-foreground ml-1">{log.actorName}</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground">Role:</span>
                    <span className="text-foreground ml-1">{log.roles.join(", ") || "-"}</span>
                  </div>
                  <div>
                    <span className="text-muted-foreground">Entity:</span>
                    <span className="text-foreground ml-1 font-mono">
                      {log.entityType}:{log.entityId}
                    </span>
                  </div>
                  <div>
                    <span className="text-muted-foreground">Correlation:</span>
                    <span className="text-foreground ml-1 font-mono">{log.correlationId}</span>
                  </div>
                </div>

                {log.reason && (
                  <div className="mt-2 text-[10px]">
                    <span className="text-muted-foreground">Reason:</span>
                    <span className="text-foreground ml-1 italic">{log.reason}</span>
                  </div>
                )}

                <div className="mt-2 grid gap-4 md:grid-cols-2 text-[9px]">
                  <div className="rounded bg-secondary/50 p-2">
                    <span className="text-muted-foreground font-medium">Before:</span>
                    <pre className="text-foreground font-mono mt-0.5 whitespace-pre-wrap">
                      {JSON.stringify(log.beforePayload, null, 2)}
                    </pre>
                  </div>
                  <div className="rounded bg-secondary/50 p-2">
                    <span className="text-muted-foreground font-medium">After:</span>
                    <pre className="text-foreground font-mono mt-0.5 whitespace-pre-wrap">
                      {JSON.stringify(log.afterPayload, null, 2)}
                    </pre>
                  </div>
                </div>

                <div className="flex gap-2 mt-3">
                  <Button size="sm" variant="ghost" className="h-7 text-[10px]" onClick={() => setSelectedLog(log)}>
                    <Eye className="w-3 h-3 mr-1" /> Full Detail
                  </Button>
                  <Button
                    size="sm"
                    variant="ghost"
                    className="h-7 text-[10px]"
                    onClick={() => void toggleMutation.mutate({ auditLogId: log.auditLogId, followUp: !log.followUp })}
                    disabled={toggleMutation.isPending}
                  >
                    <Flag className="w-3 h-3 mr-1" /> Toggle Follow-up
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <Dialog open={!!selectedLog} onOpenChange={() => setSelectedLog(null)}>
        <DialogContent className="sm:max-w-3xl">
          <DialogHeader>
            <DialogTitle>Audit Record Detail</DialogTitle>
            <DialogDescription>
              Review the full before and after state, correlation ID, and follow-up flag.
            </DialogDescription>
          </DialogHeader>
          {selectedLog && (
            <div className="space-y-4 text-xs">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <span className="text-muted-foreground">Action:</span>
                  <span className="ml-2 text-foreground font-mono">{selectedLog.action}</span>
                </div>
                <div>
                  <span className="text-muted-foreground">Recorded:</span>
                  <span className="ml-2 text-foreground">{selectedLog.recordedAt}</span>
                </div>
                <div>
                  <span className="text-muted-foreground">Actor:</span>
                  <span className="ml-2 text-foreground">{selectedLog.actorName}</span>
                </div>
                <div>
                  <span className="text-muted-foreground">IP Address:</span>
                  <span className="ml-2 text-foreground font-mono">{selectedLog.ipAddress ?? "-"}</span>
                </div>
                <div className="col-span-2">
                  <span className="text-muted-foreground">Correlation ID:</span>
                  <span className="ml-2 text-foreground font-mono">{selectedLog.correlationId}</span>
                </div>
              </div>
              <div className="grid gap-4 md:grid-cols-2">
                <div className="rounded border border-border bg-secondary/20 p-3">
                  <p className="mb-2 text-muted-foreground font-medium">Before</p>
                  <pre className="whitespace-pre-wrap font-mono text-foreground">{JSON.stringify(selectedLog.beforePayload, null, 2)}</pre>
                </div>
                <div className="rounded border border-border bg-secondary/20 p-3">
                  <p className="mb-2 text-muted-foreground font-medium">After</p>
                  <pre className="whitespace-pre-wrap font-mono text-foreground">{JSON.stringify(selectedLog.afterPayload, null, 2)}</pre>
                </div>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}

function resolveAuditDateRange(datePreset: string, customStartDate: string, customEndDate: string) {
  const today = new Date();
  const endDate = today.toISOString().slice(0, 10);

  if (datePreset === "today") {
    return { startDate: endDate, endDate };
  }
  if (datePreset === "last7") {
    const start = new Date(today);
    start.setDate(today.getDate() - 6);
    return { startDate: start.toISOString().slice(0, 10), endDate };
  }
  if (datePreset === "last30") {
    const start = new Date(today);
    start.setDate(today.getDate() - 29);
    return { startDate: start.toISOString().slice(0, 10), endDate };
  }
  if (datePreset === "custom") {
    return { startDate: customStartDate || "", endDate: customEndDate || "" };
  }
  return { startDate: "", endDate: "" };
}
