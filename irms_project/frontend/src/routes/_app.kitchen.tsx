import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { SectionHeader } from "@/components/shared/SectionHeader";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { Button } from "@/components/ui/button";
import { Flame, Clock, AlertTriangle, Zap, Check, Ban, ArrowUp, RotateCcw } from "lucide-react";
import type { KitchenStationType } from "@/shared/types";
import { toast } from "sonner";
import { apiFetch } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import { mapKitchenData } from "@/lib/api/mappers";
import { useAuth } from "@/lib/auth";
import type { KitchenOverviewResponse } from "@/lib/api/types";

export const Route = createFileRoute("/_app/kitchen")({
  component: KitchenPage,
  head: () => ({ meta: [{ title: "Kitchen Display - IRMS" }] }),
});

type KitchenItemStatus = "queued" | "cooking" | "ready" | "blocked" | "hold_for_service" | "served";
type KitchenPriority = "normal" | "rush" | "expedite";

interface KitchenItem {
  id: string;
  orderItemId: string;
  name: string;
  qty: number;
  mods: string[];
  allergy?: string;
  specialInstructions?: string;
  status: KitchenItemStatus;
  station: KitchenStationType;
  priority: boolean;
}

interface KitchenOrder {
  id: string;
  orderId: string;
  table: number;
  server: string;
  priority: KitchenPriority;
  status: KitchenItemStatus;
  createdAt: number;
  items: KitchenItem[];
}

const priorityStyles: Record<KitchenPriority, string> = {
  normal: "",
  rush: "ring-2 ring-amber-300",
  expedite: "ring-2 ring-red-400 animate-pulse-urgent",
};

function KitchenPage() {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const [station, setStation] = useState<KitchenStationType | "all">("all");
  const [now, setNow] = useState(Date.now());
  const [pendingActionKey, setPendingActionKey] = useState<string | null>(null);
  const [showCancelOrder, setShowCancelOrder] = useState<string | null>(null);
  const [showCancelItem, setShowCancelItem] = useState<{ ticketId: string; itemId: string } | null>(null);
  const [showReturnOrder, setShowReturnOrder] = useState<string | null>(null);
  const [showPriorityDialog, setShowPriorityDialog] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ["kitchen", station],
    queryFn: () =>
      apiFetch<KitchenOverviewResponse>(
        station === "all" ? ENDPOINTS.kitchen.overview : `${ENDPOINTS.kitchen.overview}?station=${station}`,
      ),
    refetchInterval: 5000,
  });

  const canApproveKitchenCancel = Boolean(
    user?.permissions.includes("all") || user?.roles.some((role) => role === "manager" || role === "admin"),
  );

  useEffect(() => {
    const interval = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(interval);
  }, []);

  const overview = data ? mapKitchenData(data) : null;
  const stations = useMemo(
    () => ["all", ...((overview?.stations ?? []) as KitchenStationType[])],
    [overview?.stations],
  );
  const orders: KitchenOrder[] = (overview?.tickets ?? []).map((ticket) => ({
    id: ticket.id,
    orderId: ticket.orderId,
    table: ticket.tableNumber,
    server: ticket.serverName,
    priority: normalizePriority(ticket.priority),
    status: normalizeKitchenStatus(ticket.status),
    createdAt: Date.parse(ticket.createdAt),
    items: ticket.items.map((item) => ({
      id: item.id,
      orderItemId: item.orderItemId,
      name: item.name,
      qty: item.quantity,
      mods: item.modifiers ?? [],
      allergy: item.allergyNotes ?? undefined,
      specialInstructions: item.specialInstructions ?? undefined,
      status: normalizeKitchenStatus(item.status),
      station: item.station as KitchenStationType,
      priority: normalizePriority(ticket.priority) !== "normal",
    })),
  }));

  const filtered = useMemo(() => {
    if (station === "all") return orders;
    return orders
      .map((order) => ({
        ...order,
        items: order.items.filter((item) => item.station === station),
      }))
      .filter((order) => order.items.length > 0);
  }, [orders, station]);

  const sorted = useMemo(
    () =>
      [...filtered].sort((left, right) => {
        const priorityRank = { expedite: 0, rush: 1, normal: 2 };
        if (priorityRank[left.priority] !== priorityRank[right.priority]) {
          return priorityRank[left.priority] - priorityRank[right.priority];
        }
        return left.createdAt - right.createdAt;
      }),
    [filtered],
  );

  const refreshKitchen = async (message: string) => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["kitchen"] }),
      queryClient.invalidateQueries({ queryKey: ["orders"] }),
      queryClient.invalidateQueries({ queryKey: ["inventory"] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
      queryClient.invalidateQueries({ queryKey: ["billing"] }),
    ]);
    toast.success(message);
  };

  const actionMutation = useMutation({
    mutationFn: async (request: { path: string; method?: "PATCH" | "POST"; body?: unknown; successMessage: string }) =>
      apiFetch(request.path, {
        method: request.method ?? "PATCH",
        body: request.body === undefined ? undefined : JSON.stringify(request.body),
      }),
    onMutate: (variables) => {
      setPendingActionKey(createActionKey(variables.path, variables.body));
    },
    onSuccess: async (_, variables) => {
      await refreshKitchen(variables.successMessage);
    },
    onError: (error: Error) => toast.error(error.message),
    onSettled: () => setPendingActionKey(null),
  });

  const getElapsed = (createdAt: number) => {
    if (!Number.isFinite(createdAt) || createdAt <= 0) return "Check time";
    const minutes = Math.floor((now - createdAt) / 60000);
    if (minutes < 0) return "Check time";
    if (minutes >= 120) return "Over 2h";
    const seconds = Math.floor(((now - createdAt) % 60000) / 1000);
    return `${minutes}:${seconds.toString().padStart(2, "0")}`;
  };

  const getUrgencyColor = (createdAt: number) => {
    if (!Number.isFinite(createdAt) || createdAt <= 0) return "text-muted-foreground";
    const minutes = (now - createdAt) / 60000;
    if (minutes < 0 || minutes >= 120) return "text-muted-foreground";
    if (minutes > 20) return "text-red-600";
    if (minutes > 10) return "text-amber-600";
    return "text-emerald-600";
  };

  const statusBadgeVariant = (status: KitchenItemStatus) => {
    switch (status) {
      case "ready":
        return "success" as const;
      case "cooking":
        return "warning" as const;
      case "queued":
        return "info" as const;
      case "blocked":
        return "danger" as const;
      case "served":
        return "success" as const;
      default:
        return "neutral" as const;
    }
  };

  const isActionPending = (path: string, body?: unknown) =>
    actionMutation.isPending && pendingActionKey === createActionKey(path, body);

  return (
    <div className="space-y-4">
      <SectionHeader
        title="Kitchen"
        description="Track the queue, start dishes when a station is ready, and hand off finished plates."
      />

      <div className="flex items-center gap-4 text-xs">
        <span className="flex items-center gap-1.5 text-emerald-600"><span className="w-2 h-2 rounded-full bg-emerald-500" />{"<"}10m</span>
        <span className="flex items-center gap-1.5 text-amber-600"><span className="w-2 h-2 rounded-full bg-amber-500" />10-20m</span>
        <span className="flex items-center gap-1.5 text-red-600"><span className="w-2 h-2 rounded-full bg-red-500" />{">"}20m</span>
      </div>

      <div className="flex gap-1 bg-muted p-1 rounded-lg overflow-x-auto">
        {stations.map((stationName) => {
          const count = stationName === "all"
            ? orders.length
            : orders.filter((order) => order.items.some((item) => item.station === stationName)).length;
          return (
            <button
              key={stationName}
              onClick={() => setStation(stationName as KitchenStationType | "all")}
              className={`px-4 py-2 rounded-md text-sm font-medium capitalize transition-colors whitespace-nowrap ${station === stationName ? "bg-card text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground"}`}
            >
              {stationName === "all" ? `ALL (${count})` : `${stationName} (${count})`}
            </button>
          );
        })}
      </div>

      {isLoading ? (
        <div className="py-12 text-center text-sm text-muted-foreground">Loading kitchen tickets...</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {sorted.map((order) => (
            <div key={order.id} className={`bg-card rounded-xl border border-border shadow-sm p-4 ${priorityStyles[order.priority]}`}>
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <span className="text-lg font-heading font-bold text-foreground">T{order.table}</span>
                  <span className="text-xs text-muted-foreground font-mono">{order.id}</span>
                </div>
                <div className="flex items-center gap-2">
                  {order.priority !== "normal" && (
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase ${
                        order.priority === "expedite" ? "bg-red-100 text-red-700" : "bg-amber-100 text-amber-700"
                      }`}
                    >
                      {order.priority === "expedite" && <Zap className="w-3 h-3 inline mr-0.5" />}
                      {order.priority}
                    </span>
                  )}
                  <span className={`text-sm font-mono font-bold ${getUrgencyColor(order.createdAt)}`}>
                    <Clock className="w-3.5 h-3.5 inline mr-0.5" />
                    {getElapsed(order.createdAt)}
                  </span>
                </div>
              </div>

              <p className="text-xs text-muted-foreground mb-2">{order.server}</p>
              {order.status === "queued" && (
                <p className="text-[10px] text-muted-foreground mb-2">Cooking will start shortly.</p>
              )}

              <div className="space-y-2">
                {order.items.map((item) => (
                  <div key={item.id} className={`p-2.5 rounded-lg border ${item.priority ? "border-purple-300 bg-purple-50" : "border-border/50 bg-muted/30"}`}>
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-1">
                          <span className="text-sm font-medium text-foreground">{item.qty}x {item.name}</span>
                          {item.priority && <ArrowUp className="w-3 h-3 text-purple-600" />}
                        </div>
                        {item.mods.length > 0 && <p className="text-[10px] text-muted-foreground mt-0.5">{item.mods.join(", ")}</p>}
                        {item.specialInstructions && <p className="text-[10px] text-muted-foreground mt-0.5">{item.specialInstructions}</p>}
                        {item.allergy && (
                          <p className="text-[10px] font-bold text-red-700 mt-1 flex items-center gap-1 bg-red-50 px-1.5 py-0.5 rounded border border-red-200">
                            <AlertTriangle className="w-3 h-3" />
                            ALLERGY: {item.allergy}
                          </p>
                        )}
                      </div>
                      <StatusBadge status={item.status.replace("_", " ")} variant={statusBadgeVariant(item.status)} />
                    </div>
                    {item.status !== "ready" && item.status !== "blocked" && item.status !== "served" && (
                      <div className="flex gap-1 mt-2">
                        {item.status === "queued" && (
                          <Button
                            size="sm"
                            className="h-6 text-[10px]"
                            disabled={isActionPending(ENDPOINTS.kitchen.itemStatus(item.id), { status: "cooking", reason: null })}
                            onClick={() =>
                              void actionMutation.mutate({
                                path: ENDPOINTS.kitchen.itemStatus(item.id),
                                body: { status: "cooking", reason: null },
                                successMessage: "Item moved to cooking",
                              })
                            }
                          >
                            Begin Cooking
                          </Button>
                        )}
                        {item.status === "cooking" && (
                          <Button
                            size="sm"
                            variant="outline"
                            className="h-6 text-[10px] text-emerald-600"
                            disabled={isActionPending(ENDPOINTS.kitchen.itemStatus(item.id), { status: "ready", reason: null })}
                            onClick={() =>
                              void actionMutation.mutate({
                                path: ENDPOINTS.kitchen.itemStatus(item.id),
                                body: { status: "ready", reason: null },
                                successMessage: "Item marked ready",
                              })
                            }
                          >
                            Mark Ready
                          </Button>
                        )}
                        <Button
                          size="sm"
                          variant="ghost"
                          className="h-6 text-[10px] text-destructive"
                          disabled={actionMutation.isPending || (item.status === "cooking" && !canApproveKitchenCancel)}
                          onClick={() => setShowCancelItem({ ticketId: order.id, itemId: item.id })}
                        >
                          Cancel
                        </Button>
                        <Button
                          size="sm"
                          variant="ghost"
                          className="h-6 text-[10px]"
                          disabled={actionMutation.isPending}
                          onClick={() => setShowPriorityDialog(order.id)}
                        >
                          <ArrowUp className="w-3 h-3" />
                        </Button>
                      </div>
                    )}
                  </div>
                ))}
              </div>

              <div className="flex gap-2 mt-3 pt-3 border-t border-border">
                {order.status === "queued" && (
                  <Button
                    size="sm"
                    className="flex-1 h-8"
                    disabled={isActionPending(ENDPOINTS.kitchen.ticketStatus(order.id), { status: "cooking", reason: null })}
                    onClick={() =>
                      void actionMutation.mutate({
                        path: ENDPOINTS.kitchen.ticketStatus(order.id),
                        body: { status: "cooking", reason: null },
                        successMessage: "Ticket moved to cooking",
                      })
                    }
                  >
                    <Flame className="w-3.5 h-3.5 mr-1" /> Begin Cooking
                  </Button>
                )}
                {order.status === "served" && (
                  <Button
                    size="sm"
                    variant="outline"
                    className="flex-1 h-8 text-amber-600 border-amber-300"
                    disabled={actionMutation.isPending}
                    onClick={() => setShowReturnOrder(order.id)}
                  >
                    <RotateCcw className="w-3.5 h-3.5 mr-1" /> Return
                  </Button>
                )}
                {order.status === "ready" && (
                  <Button
                    size="sm"
                    variant="outline"
                    className="flex-1 h-8 text-emerald-600 border-emerald-300"
                    disabled={isActionPending(ENDPOINTS.kitchen.serve(order.id))}
                    onClick={() =>
                      void actionMutation.mutate({
                        path: ENDPOINTS.kitchen.serve(order.id),
                        method: "POST",
                        successMessage: "Ticket served and handed off",
                      })
                    }
                  >
                    <Check className="w-3.5 h-3.5 mr-1" /> Mark Served
                  </Button>
                )}
                <Button
                  size="sm"
                  variant="ghost"
                  className="h-8"
                  disabled={actionMutation.isPending}
                  onClick={() => setShowPriorityDialog(order.id)}
                  title="Toggle Priority"
                >
                  <ArrowUp className="w-3.5 h-3.5" />
                </Button>
                {order.status !== "ready" && order.status !== "served" && (
                  <Button
                    size="sm"
                    variant="ghost"
                    className="h-8 text-destructive"
                    disabled={actionMutation.isPending || (order.status === "cooking" && !canApproveKitchenCancel)}
                    onClick={() => setShowCancelOrder(order.id)}
                  >
                    <Ban className="w-3.5 h-3.5" />
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      <ConfirmDialog
        open={!!showPriorityDialog}
        onOpenChange={() => setShowPriorityDialog(null)}
        title="Update Ticket Priority"
        description="A priority reason is required for rush or expedite handling."
        confirmLabel="Update Priority"
        requireReason
        reasonLabel="Priority Reason"
        onConfirm={(reason) => {
          if (showPriorityDialog) {
            void actionMutation.mutate({
              path: ENDPOINTS.kitchen.priority(showPriorityDialog),
              method: "POST",
              body: { reason: reason ?? "" },
              successMessage: "Ticket priority updated",
            });
          }
          setShowPriorityDialog(null);
        }}
      />

      <ConfirmDialog
        open={!!showCancelOrder}
        onOpenChange={() => setShowCancelOrder(null)}
        title="Cancel Order"
        description="Use this only when the kitchen should stop the ticket. Manager approval is required once cooking has started."
        confirmLabel="Cancel Order"
        variant="destructive"
        requireReason
        reasonLabel="Cancellation Reason"
        onConfirm={(reason) => {
          if (showCancelOrder) {
            void actionMutation.mutate({
              path: ENDPOINTS.kitchen.cancel(showCancelOrder),
              method: "POST",
              body: { status: "blocked", reason: reason ?? "" },
              successMessage: "Ticket cancelled",
            });
          }
          setShowCancelOrder(null);
        }}
      />

      <ConfirmDialog
        open={!!showReturnOrder}
        onOpenChange={() => setShowReturnOrder(null)}
        title="Return Served Ticket"
        description="Use this when a served dish is returned because of a wrong table, quality issue, or similar recovery reason."
        confirmLabel="Return Ticket"
        requireReason
        reasonLabel="Return Reason"
        onConfirm={(reason) => {
          if (showReturnOrder) {
            void actionMutation.mutate({
              path: ENDPOINTS.kitchen.returnTicket(showReturnOrder),
              method: "POST",
              body: { reason: reason ?? "" },
              successMessage: "Ticket returned to ready state",
            });
          }
          setShowReturnOrder(null);
        }}
      />

      <ConfirmDialog
        open={!!showCancelItem}
        onOpenChange={() => setShowCancelItem(null)}
        title="Cancel Item"
        description="Use this only when the kitchen should stop the dish. Manager approval is required once cooking has started."
        confirmLabel="Cancel Item"
        variant="destructive"
        requireReason
        reasonLabel="Cancellation Reason"
        onConfirm={(reason) => {
          if (showCancelItem) {
            void actionMutation.mutate({
              path: ENDPOINTS.kitchen.itemStatus(showCancelItem.itemId),
              body: { status: "blocked", reason: reason ?? "" },
              successMessage: "Item cancelled",
            });
          }
          setShowCancelItem(null);
        }}
      />
    </div>
  );
}

function normalizeKitchenStatus(status: string): KitchenItemStatus {
  switch (status) {
    case "new":
      return "queued";
    case "served":
      return "served";
    case "blocked":
      return "blocked";
    case "hold_for_service":
      return "hold_for_service";
    case "ready":
      return "ready";
    case "cooking":
      return "cooking";
    case "started":
      return "cooking";
    default:
      return "queued";
  }
}

function normalizePriority(priority: string): KitchenPriority {
  if (priority === "rush" || priority === "expedite") {
    return priority;
  }
  return "normal";
}

function createActionKey(path: string, body?: unknown) {
  return `${path}:${JSON.stringify(body ?? null)}`;
}
