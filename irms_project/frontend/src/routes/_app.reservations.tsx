import { createFileRoute, Link } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { SectionHeader } from "@/components/shared/SectionHeader";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { FormDialog } from "@/components/shared/FormDialog";
import { NotificationDialog } from "@/components/shared/NotificationDialog";
import { EmptyState } from "@/components/shared/EmptyState";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  CalendarDays, Users, MapPin, Clock, Plus, Search, Edit, Trash2,
  Bell, Check, X, UserPlus, CreditCard, ShoppingCart,
} from "lucide-react";
import type { TableStatus } from "@/shared/types";
import { toast } from "sonner";
import { apiFetch } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import { mapReservationData } from "@/lib/api/mappers";
import { useAuth } from "@/lib/auth";
import { hasPermission } from "@/lib/permissions";
import type { ReservationOverviewResponse, ReservationRecommendationResponse } from "@/lib/api/types";

export const Route = createFileRoute("/_app/reservations")({
  component: ReservationsPage,
  head: () => ({ meta: [{ title: "Reservations & Tables - IRMS" }] }),
});

interface TableData {
  id: string;
  number: number;
  capacity: number;
  status: TableStatus;
  notes: string;
  checkinTime?: string;
  reservationTime?: string;
}

interface ReservationData {
  id: string;
  guest: string;
  phone: string;
  email: string;
  party: number;
  date: string;
  time: string;
  status: "pending" | "confirmed" | "checked_in" | "no_show" | "cancelled";
  table: number | null;
  notes: string;
}

interface WaitlistData {
  id: string;
  name: string;
  phone: string;
  email: string;
  party: number;
  wait: string;
  status: "waiting" | "notified" | "skipped" | "expired" | "seated" | "left";
  holdExpiresAt?: string | null;
  notes: string;
  tablesAvailableNow: number;
  suggestedTable: number | null;
}

interface NotificationTargetState {
  id: string;
  name: string;
  type: "reservation" | "waitlist";
  email?: string;
}

type ReservationTab = "reservations" | "tables" | "waitlist";

const tableStatusColor: Record<TableStatus, string> = {
  available: "bg-emerald-50 border-emerald-300 text-emerald-700",
  reserved: "bg-amber-50 border-amber-300 text-amber-700",
  occupied: "bg-blue-50 border-blue-300 text-blue-700",
  cleaning: "bg-gray-100 border-gray-300 text-gray-500",
  out_of_service: "bg-red-50 border-red-300 text-red-500",
};

const resStatusVariant: Record<string, "success" | "warning" | "info" | "danger" | "neutral"> = {
  confirmed: "success",
  pending: "warning",
  checked_in: "info",
  no_show: "danger",
  cancelled: "neutral",
};

function ReservationsPage() {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<ReservationTab>("reservations");
  const [reservationSearch, setReservationSearch] = useState("");

  const { data, isLoading } = useQuery({
    queryKey: ["reservations"],
    queryFn: () => apiFetch<ReservationOverviewResponse>(ENDPOINTS.reservations.overview),
  });

  const overview = data ? mapReservationData(data) : null;
  const tables: TableData[] = (overview?.tables ?? []).map((table) => ({
    ...table,
    notes: table.notes ?? "",
    checkinTime: table.checkinTime ?? undefined,
    reservationTime: table.reservationTime ?? undefined,
  })) as TableData[];
  const reservations: ReservationData[] = (overview?.reservations ?? []).map((reservation) => ({
    ...reservation,
    email: reservation.email ?? "",
    notes: reservation.notes ?? "",
    table: reservation.table ?? null,
  })) as ReservationData[];
  const waitlist: WaitlistData[] = (overview?.waitlist ?? []).map((entry) => ({
    id: entry.id,
    name: entry.name,
    phone: entry.phone,
    email: entry.email ?? "",
    party: entry.party,
    wait: entry.wait,
    status: entry.status as WaitlistData["status"],
    holdExpiresAt: entry.holdExpiresAt ?? null,
    notes: entry.notes ?? "",
    tablesAvailableNow: entry.tablesAvailableNow ?? 0,
    suggestedTable: entry.suggestedTable ?? null,
  }));

  const [showNewReservation, setShowNewReservation] = useState(false);
  const [showNewTable, setShowNewTable] = useState(false);
  const [showEditTable, setShowEditTable] = useState<TableData | null>(null);
  const [showDeleteTable, setShowDeleteTable] = useState<TableData | null>(null);
  const [showTableStatus, setShowTableStatus] = useState<TableData | null>(null);
  const [showEditReservation, setShowEditReservation] = useState<ReservationData | null>(null);
  const [showCheckInReservation, setShowCheckInReservation] = useState<ReservationData | null>(null);
  const [showNotification, setShowNotification] = useState<NotificationTargetState | null>(null);
  const [showAddWaitlist, setShowAddWaitlist] = useState(false);

  const [newRes, setNewRes] = useState({
    guest: "",
    phone: "",
    email: "",
    party: 2,
    date: "",
    time: "",
    notes: "",
    tableId: null as string | null,
  });
  const [newTable, setNewTable] = useState({ number: 0, capacity: 2, notes: "" });
  const [newWaitlist, setNewWaitlist] = useState({ name: "", phone: "", email: "", notes: "", party: 2 });
  const [tableStatusForm, setTableStatusForm] = useState({ targetStatus: "cleaning", reason: "" });
  const [checkInForm, setCheckInForm] = useState({ actualPartySize: 2, replacementTableId: "" });

  const { data: recommendation } = useQuery({
    queryKey: ["reservation-recommendation", showNewReservation, newRes.date, newRes.time, newRes.party],
    queryFn: () => {
      const query = new URLSearchParams({
        date: newRes.date,
        time: newRes.time,
        party: String(newRes.party),
      });
      return apiFetch<ReservationRecommendationResponse>(`${ENDPOINTS.reservations.recommendations}?${query.toString()}`);
    },
    enabled: showNewReservation && newRes.date !== "" && newRes.time !== "" && newRes.party > 0,
  });

  const filteredReservations = useMemo(
    () =>
      reservations.filter((reservation) =>
        reservationSearch === ""
          ? true
          : reservation.guest.toLowerCase().includes(reservationSearch.toLowerCase()),
      ),
    [reservationSearch, reservations],
  );

  const availableTables = recommendation?.candidateTables ?? tables.filter((table) => table.status === "available" && table.capacity >= newRes.party);
  const canManageReservations = hasPermission(user, "reservations.manage");
  const canAssignTables = hasPermission(user, "tables.assign");
  const canApproveLateRecovery = Boolean(
    user?.permissions.includes("all") || user?.roles.some((role) => role === "manager" || role === "admin"),
  );
  const checkInCandidateTables = useMemo(
    () =>
      tables.filter(
        (table) =>
          table.capacity >= checkInForm.actualPartySize
          && (table.status === "available" || table.status === "reserved"),
      ),
    [checkInForm.actualPartySize, tables],
  );

  const refreshReservations = async (message: string) => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["reservations"] }),
      queryClient.invalidateQueries({ queryKey: ["orders"] }),
      queryClient.invalidateQueries({ queryKey: ["billing"] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
    ]);
    toast.success(message);
  };

  const actionMutation = useMutation({
    mutationFn: async (request: { path: string; method?: "POST" | "PUT" | "DELETE"; body?: unknown; successMessage: string }) =>
      apiFetch(request.path, {
        method: request.method ?? "POST",
        body: request.body === undefined ? undefined : JSON.stringify(request.body),
      }),
    onSuccess: async (_, variables) => {
      await refreshReservations(variables.successMessage);
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const tableStatusMutation = useMutation({
    mutationFn: (payload: { tableId: string; targetStatus: string; reason: string }) =>
      apiFetch<{ tableId: string; status: string; message: string; suggestedWaitlistGuest?: string | null }>(
        ENDPOINTS.tables.status(payload.tableId),
        {
          method: "POST",
          body: JSON.stringify({
            targetStatus: payload.targetStatus,
            reason: payload.reason.trim() || null,
          }),
        },
      ),
    onSuccess: async (response) => {
      setShowTableStatus(null);
      setTableStatusForm({ targetStatus: "cleaning", reason: "" });
      await refreshReservations(response.message);
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const reservationsBusy = actionMutation.isPending || tableStatusMutation.isPending;

  const handleCreateReservation = () => {
    void actionMutation.mutate({
      path: ENDPOINTS.reservations.list,
      body: {
        guest: newRes.guest.trim(),
        phone: newRes.phone.trim(),
        email: newRes.email.trim() || null,
        party: newRes.party,
        date: newRes.date,
        time: newRes.time,
        notes: newRes.notes.trim() || null,
        tableId: newRes.tableId,
        fallbackToWaitlist: false,
      },
      successMessage: "Reservation created",
    });
    setNewRes({ guest: "", phone: "", email: "", party: 2, date: "", time: "", notes: "", tableId: null });
    setShowNewReservation(false);
  };

  const handleAddWaitlistFromReservation = () => {
    if (!newRes.guest.trim() || !newRes.phone.trim()) {
      toast.error("Customer name and phone number are required to add the guest to the waitlist.");
      return;
    }
    void actionMutation.mutate({
      path: ENDPOINTS.waitlist.create,
      body: {
        name: newRes.guest.trim(),
        phone: newRes.phone.trim(),
        email: newRes.email.trim() || null,
        notes: newRes.notes.trim() || null,
        party: newRes.party,
      },
      successMessage: "Added to waitlist",
    });
    setNewRes({ guest: "", phone: "", email: "", party: 2, date: "", time: "", notes: "", tableId: null });
    setShowNewReservation(false);
  };

  const handleUpdateReservation = () => {
    if (!showEditReservation) return;
    void actionMutation.mutate({
      path: ENDPOINTS.reservations.update(showEditReservation.id),
      method: "PUT",
      body: {
        guest: showEditReservation.guest.trim(),
        party: showEditReservation.party,
        notes: showEditReservation.notes.trim() || null,
      },
      successMessage: "Reservation updated",
    });
    setShowEditReservation(null);
  };

  const handleCheckInReservation = () => {
    if (!showCheckInReservation) return;
    void actionMutation.mutate({
      path: ENDPOINTS.reservations.checkIn(showCheckInReservation.id),
      body: {
        approveRecovery: canApproveLateRecovery,
        actualPartySize: checkInForm.actualPartySize,
        replacementTableId: checkInForm.replacementTableId || null,
      },
      successMessage: "Guest checked in",
    });
    setShowCheckInReservation(null);
    setCheckInForm({ actualPartySize: 2, replacementTableId: "" });
  };

  const handleCreateTable = () => {
    void actionMutation.mutate({
      path: ENDPOINTS.tables.create,
      body: {
        number: newTable.number,
        capacity: newTable.capacity,
        notes: newTable.notes.trim() || null,
      },
      successMessage: "Table added",
    });
    setNewTable({ number: 0, capacity: 2, notes: "" });
    setShowNewTable(false);
  };

  const handleUpdateTable = () => {
    if (!showEditTable) return;
    void actionMutation.mutate({
      path: ENDPOINTS.tables.update(showEditTable.id),
      method: "PUT",
      body: {
        number: showEditTable.number,
        capacity: showEditTable.capacity,
        notes: showEditTable.notes.trim() || null,
      },
      successMessage: "Table updated",
    });
    setShowEditTable(null);
  };

  const handleDeleteTable = () => {
    if (!showDeleteTable) return;
    void actionMutation.mutate({
      path: ENDPOINTS.tables.delete(showDeleteTable.id),
      method: "DELETE",
      successMessage: "Table deleted",
    });
    setShowDeleteTable(null);
  };

  const handleUpdateTableStatus = () => {
    if (!showTableStatus) return;
    void tableStatusMutation.mutate({
      tableId: showTableStatus.id,
      targetStatus: tableStatusForm.targetStatus,
      reason: tableStatusForm.reason,
    });
  };

  const handleAddWaitlist = () => {
    void actionMutation.mutate({
      path: ENDPOINTS.waitlist.create,
      body: {
        name: newWaitlist.name.trim(),
        phone: newWaitlist.phone.trim(),
        email: newWaitlist.email.trim() || null,
        notes: newWaitlist.notes.trim() || null,
        party: newWaitlist.party,
      },
      successMessage: "Added to waitlist",
    });
    setNewWaitlist({ name: "", phone: "", email: "", notes: "", party: 2 });
    setShowAddWaitlist(false);
  };

  const notificationChannels = showNotification
    ? [
        { label: "SMS", value: "sms" as const },
        ...(showNotification.email ? [{ label: "Email", value: "email" as const }] : []),
        { label: "In-App Notice", value: "in_app" as const },
      ]
    : [];

  return (
    <div className="space-y-6">
      <SectionHeader
        title="Reservations & Tables"
        description="Manage bookings, seat guests smoothly, and keep the waitlist moving."
        actions={canManageReservations ? (
          <Button onClick={() => setShowNewReservation(true)}>
            <Plus className="w-4 h-4 mr-2" />
            New Reservation
          </Button>
        ) : undefined}
      />

      <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-semibold text-foreground flex items-center gap-2">
            <MapPin className="w-4 h-4 text-primary" />
            Floor Map - Main Dining
          </h2>
          <div className="flex gap-3 text-[11px]">
            {(["available", "reserved", "occupied", "cleaning", "out_of_service"] as TableStatus[]).map((status) => (
              <span key={status} className="flex items-center gap-1.5 text-muted-foreground">
                <span className={`w-2.5 h-2.5 rounded-sm ${tableStatusColor[status].split(" ")[0]}`} />
                {status.replace("_", " ")}
              </span>
            ))}
          </div>
        </div>
        {isLoading ? (
          <div className="py-10 text-center text-sm text-muted-foreground">Loading floor map...</div>
        ) : (
          <div className="grid grid-cols-5 gap-3">
            {tables.map((table) => (
              <div
                key={table.id}
                className={`relative flex flex-col items-center justify-center p-4 rounded-xl border-2 transition-all hover:shadow-md ${tableStatusColor[table.status]}`}
              >
                <span className="text-lg font-heading font-bold">{table.number}</span>
                <span className="text-[10px] mt-0.5">{table.capacity} seats</span>
                <span className="text-[9px] mt-1 capitalize opacity-80">{table.status.replace("_", " ")}</span>
                <Link to="/tables/$tableId" params={{ tableId: table.id }} className="text-[8px] mt-2 px-1.5 py-0.5 rounded bg-white/70 text-foreground hover:bg-white">
                  View
                </Link>
                {table.checkinTime && <span className="text-[9px] mt-0.5 opacity-70">In: {table.checkinTime}</span>}
                {table.reservationTime && table.status === "reserved" && (
                  <span className="text-[9px] mt-0.5 opacity-70">Res: {table.reservationTime}</span>
                )}
                {table.status === "occupied" && (
                  <div className="flex gap-1 mt-2">
                    <Link
                      to="/billing"
                      search={table.sessionId ? { sessionId: table.sessionId } : {}}
                      className="text-[8px] px-1.5 py-0.5 rounded bg-blue-100 text-blue-700 hover:bg-blue-200"
                    >
                      <CreditCard className="w-3 h-3" />
                    </Link>
                    <Link
                      to="/orders"
                      search={table.sessionId ? { sessionId: table.sessionId } : {}}
                      className="text-[8px] px-1.5 py-0.5 rounded bg-blue-100 text-blue-700 hover:bg-blue-200"
                    >
                      <ShoppingCart className="w-3 h-3" />
                    </Link>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="flex gap-1 bg-muted p-1 rounded-lg w-fit">
        {[
          { key: "reservations" as const, label: "Today's Reservations", icon: CalendarDays },
          { key: "tables" as const, label: "Table Management", icon: MapPin },
          { key: "waitlist" as const, label: "Waitlist", icon: Clock },
        ].map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-colors ${
              activeTab === tab.key ? "bg-card text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground"
            }`}
          >
            <tab.icon className="w-4 h-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === "reservations" && (
        <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-foreground">Today's Reservations</h2>
            <div className="relative">
              <Search className="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
              <Input
                className="pl-8 w-56 h-8 text-sm"
                placeholder="Search guest..."
                value={reservationSearch}
                onChange={(event) => setReservationSearch(event.target.value)}
              />
            </div>
          </div>
          {isLoading ? (
            <div className="py-10 text-center text-sm text-muted-foreground">Loading reservations...</div>
          ) : filteredReservations.length === 0 ? (
            <EmptyState icon={CalendarDays} title="No reservations today" description="Create a new reservation to get started" />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border">
                    {["ID", "Guest", "Party", "Time", "Table", "Status", "Actions"].map((heading) => (
                      <th key={heading} className="text-left py-2.5 px-3 text-xs font-medium text-muted-foreground uppercase tracking-wider">
                        {heading}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {filteredReservations.map((reservation) => (
                    <tr key={reservation.id} className="border-b border-border/50 hover:bg-muted/30 transition-colors">
                      <td className="py-3 px-3 font-mono text-xs text-foreground">{reservation.id}</td>
                      <td className="py-3 px-3 text-foreground text-sm font-medium">{reservation.guest}</td>
                      <td className="py-3 px-3 text-muted-foreground text-sm">
                        <span className="flex items-center gap-1"><Users className="w-3.5 h-3.5" />{reservation.party}</span>
                      </td>
                      <td className="py-3 px-3 text-muted-foreground text-sm">
                        <span className="flex items-center gap-1"><Clock className="w-3.5 h-3.5" />{reservation.time}</span>
                      </td>
                      <td className="py-3 px-3 text-sm">{reservation.table ? `#${reservation.table}` : <span className="text-muted-foreground">-</span>}</td>
                      <td className="py-3 px-3">
                        <StatusBadge status={reservation.status.replace("_", " ")} variant={resStatusVariant[reservation.status]} />
                      </td>
                      <td className="py-3 px-3">
                        <div className="flex items-center gap-1">
                          {reservation.status === "pending" && (
                            <Button
                              size="sm"
                              variant="outline"
                              className="h-7 text-xs"
                              disabled={!canManageReservations || reservationsBusy}
                              onClick={() =>
                                void actionMutation.mutate({
                                  path: ENDPOINTS.reservations.confirm(reservation.id),
                                  successMessage: "Reservation confirmed",
                                })
                              }
                            >
                              <Check className="w-3 h-3 mr-1" /> Confirm
                            </Button>
                          )}
                          {reservation.status === "confirmed" && (
                            <Button
                              size="sm"
                              className="h-7 text-xs"
                              disabled={!canAssignTables || reservationsBusy}
                              onClick={() => {
                                setShowCheckInReservation(reservation);
                                setCheckInForm({
                                  actualPartySize: reservation.party,
                                  replacementTableId: "",
                                });
                              }}
                            >
                              <Check className="w-3 h-3 mr-1" /> Check In
                            </Button>
                          )}
                          {(reservation.status === "confirmed" || reservation.status === "pending") && (
                            <Button
                              size="sm"
                              variant="ghost"
                              className="h-7 text-xs text-destructive"
                              disabled={!canManageReservations || reservationsBusy}
                              onClick={() =>
                                void actionMutation.mutate({
                                  path: ENDPOINTS.reservations.noShow(reservation.id),
                                  successMessage: "Marked as no-show",
                                })
                              }
                            >
                              <X className="w-3 h-3 mr-1" /> No-Show
                            </Button>
                          )}
                          {canManageReservations && (
                            <>
                              <Button size="sm" variant="ghost" className="h-7 text-xs" disabled={reservationsBusy} onClick={() => setShowEditReservation(reservation)}>
                                <Edit className="w-3 h-3" />
                              </Button>
                              <Button
                                size="sm"
                                variant="ghost"
                                className="h-7 text-xs"
                                disabled={reservationsBusy}
                                onClick={() => setShowNotification({ id: reservation.id, name: reservation.guest, type: "reservation", email: reservation.email })}
                              >
                                <Bell className="w-3 h-3" />
                              </Button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {activeTab === "tables" && (
        <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-foreground">Table Management</h2>
            {canAssignTables && (
              <Button size="sm" onClick={() => setShowNewTable(true)}>
                <Plus className="w-4 h-4 mr-1" /> Add Table
              </Button>
            )}
          </div>
          {isLoading ? (
            <div className="py-10 text-center text-sm text-muted-foreground">Loading tables...</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border">
                    {["Table #", "Capacity", "Status", "Notes", "Actions"].map((heading) => (
                      <th key={heading} className="text-left py-2.5 px-3 text-xs font-medium text-muted-foreground uppercase tracking-wider">
                        {heading}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {tables.map((table) => (
                    <tr key={table.id} className="border-b border-border/50 hover:bg-muted/30 transition-colors">
                      <td className="py-3 px-3 font-medium text-foreground">#{table.number}</td>
                      <td className="py-3 px-3 text-muted-foreground">{table.capacity} seats</td>
                      <td className="py-3 px-3">
                        <StatusBadge
                          status={table.status.replace("_", " ")}
                          variant={
                            table.status === "available"
                              ? "success"
                              : table.status === "occupied"
                                ? "info"
                                : table.status === "reserved"
                                  ? "warning"
                                  : table.status === "out_of_service"
                                    ? "danger"
                                    : "neutral"
                          }
                        />
                      </td>
                      <td className="py-3 px-3 text-muted-foreground text-sm">{table.notes || "-"}</td>
                      <td className="py-3 px-3">
                        {canAssignTables ? (
                          <div className="flex gap-1">
                            <Button
                              size="sm"
                              variant="ghost"
                              className="h-7 text-[10px]"
                              disabled={reservationsBusy}
                              onClick={() => {
                                setShowTableStatus(table);
                                setTableStatusForm({
                                  targetStatus: table.status === "cleaning" ? "available" : "cleaning",
                                  reason: table.status === "cleaning" ? "Cleanup completed." : "",
                                });
                              }}
                            >
                              Status
                            </Button>
                            <Button size="sm" variant="ghost" className="h-7" disabled={reservationsBusy} onClick={() => setShowEditTable({ ...table })}>
                              <Edit className="w-3.5 h-3.5" />
                            </Button>
                            <Button size="sm" variant="ghost" className="h-7 text-destructive" disabled={reservationsBusy} onClick={() => setShowDeleteTable(table)}>
                              <Trash2 className="w-3.5 h-3.5" />
                            </Button>
                          </div>
                        ) : (
                          <span className="text-xs text-muted-foreground">View only</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {activeTab === "waitlist" && (
        <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-foreground flex items-center gap-2">
              <Clock className="w-4 h-4 text-warning" />
              Waitlist
            </h2>
            {canManageReservations && (
              <Button size="sm" onClick={() => setShowAddWaitlist(true)}>
                <UserPlus className="w-4 h-4 mr-1" /> Add to Waitlist
              </Button>
            )}
          </div>
          {isLoading ? (
            <div className="py-10 text-center text-sm text-muted-foreground">Loading waitlist...</div>
          ) : waitlist.length === 0 ? (
            <EmptyState icon={Users} title="Waitlist is clear" description="New walk-ins will appear here when tables are full." />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border">
                    {["Guest", "Party", "Wait", "Seating", "Status", "Actions"].map((heading) => (
                      <th key={heading} className="text-left py-2.5 px-3 text-xs font-medium text-muted-foreground uppercase tracking-wider">
                        {heading}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {waitlist.map((entry) => (
                    <tr key={entry.id} className="border-b border-border/50 hover:bg-muted/30">
                      <td className="py-3 px-3">
                        <div className="font-medium text-foreground">{entry.name}</div>
                        <div className="text-xs text-muted-foreground">{entry.phone}</div>
                        {entry.email && <div className="text-xs text-muted-foreground">{entry.email}</div>}
                        {entry.notes && <div className="mt-1 text-xs text-muted-foreground">{entry.notes}</div>}
                      </td>
                      <td className="py-3 px-3 text-muted-foreground">{entry.party} guests</td>
                      <td className="py-3 px-3 text-muted-foreground">{entry.wait}</td>
                      <td className="py-3 px-3 text-xs text-muted-foreground">
                        <div>{formatWaitlistSeating(entry)}</div>
                        {entry.status === "notified" && entry.holdExpiresAt && (
                          <div className="mt-1">Hold until {formatHoldDeadline(entry.holdExpiresAt)}</div>
                        )}
                      </td>
                      <td className="py-3 px-3">
                        <StatusBadge status={entry.status.replace("_", " ")} variant={waitlistStatusVariant(entry.status)} />
                      </td>
                      <td className="py-3 px-3">
                        <div className="flex gap-1">
                          {canManageReservations && (entry.status === "waiting" || entry.status === "skipped" || entry.status === "expired") && (
                            <Button
                              size="sm"
                              variant="ghost"
                              className="h-7 text-xs"
                              disabled={reservationsBusy}
                              onClick={() =>
                                void actionMutation.mutate({
                                  path: ENDPOINTS.waitlist.prioritize(entry.id),
                                  successMessage: "Waitlist guest prioritized",
                                })
                              }
                            >
                              Prioritize
                            </Button>
                          )}
                          {canManageReservations && (entry.status === "waiting" || entry.status === "skipped" || entry.status === "expired") && (
                            <Button
                              size="sm"
                              variant="outline"
                              className="h-7 text-xs"
                              disabled={reservationsBusy || entry.tablesAvailableNow < 1}
                              onClick={() =>
                                void actionMutation.mutate({
                                  path: ENDPOINTS.waitlist.notify(entry.id),
                                  successMessage: "Waitlist guest notified",
                                })
                              }
                            >
                              Notify
                            </Button>
                          )}
                          {entry.status === "notified" && (
                            <>
                              {canAssignTables && (
                                <Button
                                  size="sm"
                                  className="h-7 text-xs"
                                  disabled={reservationsBusy}
                                  onClick={() =>
                                    void actionMutation.mutate({
                                      path: ENDPOINTS.waitlist.seat(entry.id),
                                      successMessage: "Waitlist guest seated",
                                    })
                                  }
                                >
                                  Seat
                                </Button>
                              )}
                              {canManageReservations && (
                                <Button
                                  size="sm"
                                  variant="ghost"
                                  className="h-7 text-xs"
                                  disabled={reservationsBusy}
                                  onClick={() =>
                                    void actionMutation.mutate({
                                      path: ENDPOINTS.waitlist.skip(entry.id),
                                      successMessage: "Waitlist guest marked as skipped",
                                    })
                                  }
                                >
                                  Skip
                                </Button>
                              )}
                            </>
                          )}
                          {canManageReservations && (
                            <Button
                              size="sm"
                              variant="ghost"
                              className="h-7 text-xs"
                              disabled={reservationsBusy}
                              onClick={() => setShowNotification({ id: entry.id, name: entry.name, type: "waitlist", email: entry.email || undefined })}
                            >
                              <Bell className="w-3 h-3" />
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      <FormDialog
        open={showNewReservation}
        onOpenChange={setShowNewReservation}
        title="New Reservation"
        description="Capture the guest details and choose the best table for the booking."
        submitLabel="Confirm Reservation"
        onSubmit={handleCreateReservation}
        submitDisabled={!canManageReservations || !newRes.guest || !newRes.phone || !newRes.date || !newRes.time || reservationsBusy}
      >
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label>Customer Name *</Label>
            <Input value={newRes.guest} onChange={(event) => setNewRes((current) => ({ ...current, guest: event.target.value }))} placeholder="Full name" />
          </div>
          <div className="space-y-2">
            <Label>Phone Number *</Label>
            <Input value={newRes.phone} onChange={(event) => setNewRes((current) => ({ ...current, phone: event.target.value }))} placeholder="+1 555-0000" />
          </div>
          <div className="space-y-2">
            <Label>Email</Label>
            <Input value={newRes.email} onChange={(event) => setNewRes((current) => ({ ...current, email: event.target.value }))} type="email" placeholder="email@example.com" />
          </div>
          <div className="space-y-2">
            <Label>Number of Guests *</Label>
            <Input value={newRes.party} onChange={(event) => setNewRes((current) => ({ ...current, party: Number(event.target.value) || 1 }))} type="number" min={1} />
          </div>
          <div className="space-y-2">
            <Label>Date *</Label>
            <Input value={newRes.date} onChange={(event) => setNewRes((current) => ({ ...current, date: event.target.value }))} type="date" />
          </div>
          <div className="space-y-2">
            <Label>Time *</Label>
            <Input value={newRes.time} onChange={(event) => setNewRes((current) => ({ ...current, time: event.target.value }))} type="time" />
          </div>
        </div>
        <div className="space-y-2">
          <Label>Notes</Label>
          <Textarea value={newRes.notes} onChange={(event) => setNewRes((current) => ({ ...current, notes: event.target.value }))} placeholder="Special requests..." rows={2} />
        </div>
        {newRes.party > 0 && (
          <div className="space-y-2">
            <Label>Select Table (optional)</Label>
            <div className="grid grid-cols-4 gap-2">
              {availableTables.length === 0 ? (
                <div className="col-span-4 space-y-2">
                  <p className="text-sm text-muted-foreground">
                    No available tables for {newRes.party} guests.
                    {recommendation?.waitlistRecommended ? ` Estimated wait: ${recommendation.estimatedWaitMinutes} minutes.` : ""}
                  </p>
                  <Button
                    type="button"
                    variant="outline"
                    className="h-8 text-xs"
                    onClick={handleAddWaitlistFromReservation}
                    disabled={!canManageReservations || !newRes.guest || !newRes.phone || reservationsBusy}
                  >
                    Add to Waitlist Instead
                  </Button>
                </div>
              ) : (
                availableTables.map((table) => (
                  <button
                    key={table.id}
                    onClick={() => setNewRes((current) => ({ ...current, tableId: current.tableId === table.id ? null : table.id }))}
                    className={`p-3 rounded-lg border-2 text-center transition-colors ${
                      newRes.tableId === table.id ? "border-primary bg-primary/5" : "border-border hover:border-primary/30"
                    }`}
                  >
                    <span className="text-sm font-bold">#{table.number}</span>
                    <span className="text-xs text-muted-foreground block">{table.capacity} seats</span>
                  </button>
                ))
              )}
            </div>
          </div>
        )}
      </FormDialog>

      {showEditReservation && (
        <FormDialog
          open={!!showEditReservation}
          onOpenChange={() => setShowEditReservation(null)}
          title="Edit Reservation"
          onSubmit={handleUpdateReservation}
          submitDisabled={!canManageReservations || reservationsBusy}
        >
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Guest Name</Label>
              <Input
                value={showEditReservation.guest}
                onChange={(event) =>
                  setShowEditReservation((current) => (current ? { ...current, guest: event.target.value } : null))
                }
              />
            </div>
            <div className="space-y-2">
              <Label>Party Size</Label>
              <Input
                value={showEditReservation.party}
                onChange={(event) =>
                  setShowEditReservation((current) => (current ? { ...current, party: Number(event.target.value) || 1 } : null))
                }
                type="number"
                min={1}
              />
            </div>
          </div>
          <div className="space-y-2">
            <Label>Notes</Label>
            <Textarea
              value={showEditReservation.notes}
              onChange={(event) =>
                setShowEditReservation((current) => (current ? { ...current, notes: event.target.value } : null))
              }
              rows={2}
            />
          </div>
        </FormDialog>
      )}

      {showCheckInReservation && (
        <FormDialog
          open={!!showCheckInReservation}
          onOpenChange={() => setShowCheckInReservation(null)}
          title={`Check In - ${showCheckInReservation.guest}`}
          description="Confirm the actual party size and choose a replacement table only if the original assignment no longer fits."
          submitLabel="Open Table Session"
          onSubmit={handleCheckInReservation}
          submitDisabled={!canAssignTables || checkInForm.actualPartySize < 1 || reservationsBusy}
        >
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Actual Party Size</Label>
              <Input
                type="number"
                min={1}
                value={checkInForm.actualPartySize}
                onChange={(event) =>
                  setCheckInForm((current) => ({
                    ...current,
                    actualPartySize: Number(event.target.value) || 1,
                  }))
                }
              />
            </div>
            <div className="space-y-2">
              <Label>Replacement Table</Label>
              <select
                value={checkInForm.replacementTableId}
                onChange={(event) =>
                  setCheckInForm((current) => ({
                    ...current,
                    replacementTableId: event.target.value,
                  }))
                }
                className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm"
              >
                <option value="">Use current / auto-selected table</option>
                {checkInCandidateTables.map((table) => (
                  <option key={table.id} value={table.id}>
                    Table {table.number} - {table.capacity} seats
                  </option>
                ))}
              </select>
            </div>
          </div>
        </FormDialog>
      )}

      <FormDialog
        open={showNewTable}
        onOpenChange={setShowNewTable}
        title="Add Table"
        onSubmit={handleCreateTable}
        submitDisabled={!canAssignTables || newTable.number <= 0 || newTable.capacity <= 0 || reservationsBusy}
      >
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label>Table Number *</Label>
            <Input value={newTable.number || ""} onChange={(event) => setNewTable((current) => ({ ...current, number: Number(event.target.value) || 0 }))} type="number" min={1} />
          </div>
          <div className="space-y-2">
            <Label>Capacity *</Label>
            <Input value={newTable.capacity} onChange={(event) => setNewTable((current) => ({ ...current, capacity: Number(event.target.value) || 1 }))} type="number" min={1} />
          </div>
        </div>
        <div className="space-y-2">
          <Label>Notes</Label>
          <Textarea value={newTable.notes} onChange={(event) => setNewTable((current) => ({ ...current, notes: event.target.value }))} placeholder="Location, special features..." rows={2} />
        </div>
      </FormDialog>

      {showEditTable && (
        <FormDialog open={!!showEditTable} onOpenChange={() => setShowEditTable(null)} title="Edit Table" onSubmit={handleUpdateTable} submitDisabled={!canAssignTables || reservationsBusy}>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Table Number</Label>
              <Input value={showEditTable.number} onChange={(event) => setShowEditTable((current) => (current ? { ...current, number: Number(event.target.value) || 0 } : null))} type="number" />
            </div>
            <div className="space-y-2">
              <Label>Capacity</Label>
              <Input value={showEditTable.capacity} onChange={(event) => setShowEditTable((current) => (current ? { ...current, capacity: Number(event.target.value) || 1 } : null))} type="number" />
            </div>
          </div>
          <div className="space-y-2">
            <Label>Notes</Label>
            <Textarea value={showEditTable.notes} onChange={(event) => setShowEditTable((current) => (current ? { ...current, notes: event.target.value } : null))} rows={2} />
          </div>
        </FormDialog>
      )}

      {showTableStatus && (
        <FormDialog
          open={!!showTableStatus}
          onOpenChange={() => setShowTableStatus(null)}
          title={`Update Table #${showTableStatus.number} Status`}
          description="Use cleaning to release a settled table, then mark it available after cleanup."
          onSubmit={handleUpdateTableStatus}
          submitDisabled={!canAssignTables || tableStatusMutation.isPending}
        >
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Target Status</Label>
              <select
                value={tableStatusForm.targetStatus}
                onChange={(event) => setTableStatusForm((current) => ({ ...current, targetStatus: event.target.value }))}
                className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm"
              >
                {showTableStatus.status === "cleaning" ? (
                  <>
                    <option value="available">Available</option>
                    <option value="out_of_service">Out of Service</option>
                  </>
                ) : (
                  <>
                    <option value="cleaning">Cleaning</option>
                    <option value="out_of_service">Out of Service</option>
                  </>
                )}
              </select>
            </div>
            <div className="space-y-2">
              <Label>Reason</Label>
              <Textarea
                value={tableStatusForm.reason}
                onChange={(event) => setTableStatusForm((current) => ({ ...current, reason: event.target.value }))}
                rows={2}
                placeholder="Optional release or maintenance reason"
              />
            </div>
          </div>
        </FormDialog>
      )}

      <ConfirmDialog
        open={!!showDeleteTable}
        onOpenChange={() => setShowDeleteTable(null)}
        title="Delete Table"
        description={`Are you sure you want to delete Table #${showDeleteTable?.number}? This action cannot be undone.`}
        confirmLabel="Delete"
        variant="destructive"
        onConfirm={handleDeleteTable}
      />

      <FormDialog
        open={showAddWaitlist}
        onOpenChange={setShowAddWaitlist}
        title="Add to Waitlist"
        description="Record the party and contact details so the next open table can be matched quickly."
        onSubmit={handleAddWaitlist}
        submitDisabled={!canManageReservations || !newWaitlist.name || !newWaitlist.phone || newWaitlist.party < 1 || reservationsBusy}
      >
        <div className="space-y-4">
          <div className="space-y-2">
            <Label>Name *</Label>
            <Input value={newWaitlist.name} onChange={(event) => setNewWaitlist((current) => ({ ...current, name: event.target.value }))} />
          </div>
          <div className="space-y-2">
            <Label>Phone *</Label>
            <Input value={newWaitlist.phone} onChange={(event) => setNewWaitlist((current) => ({ ...current, phone: event.target.value }))} />
          </div>
          <div className="space-y-2">
            <Label>Email</Label>
            <Input value={newWaitlist.email} onChange={(event) => setNewWaitlist((current) => ({ ...current, email: event.target.value }))} />
          </div>
          <div className="space-y-2">
            <Label>Party Size</Label>
            <Input value={newWaitlist.party} onChange={(event) => setNewWaitlist((current) => ({ ...current, party: Number(event.target.value) || 1 }))} type="number" min={1} />
          </div>
          <div className="space-y-2">
            <Label>Notes</Label>
            <Textarea value={newWaitlist.notes} onChange={(event) => setNewWaitlist((current) => ({ ...current, notes: event.target.value }))} rows={2} placeholder="High chair, access needs, or seating notes" />
          </div>
        </div>
      </FormDialog>

      {showNotification && (
        <NotificationDialog
          open={!!showNotification}
          onOpenChange={() => setShowNotification(null)}
          recipientName={showNotification.name}
          availableChannels={notificationChannels}
          onSend={(message, channel) => {
            void actionMutation.mutate({
              path: ENDPOINTS.notifications,
              body: {
                reservationId: showNotification.type === "reservation" ? showNotification.id : null,
                waitlistEntryId: showNotification.type === "waitlist" ? showNotification.id : null,
                title: showNotification.type === "reservation" ? "Reservation update" : "Waitlist update",
                body: message,
                channel,
              },
              successMessage: `Notification queued for ${showNotification.name}`,
            });
          }}
        />
      )}
    </div>
  );
}

function formatHoldDeadline(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
}

function waitlistStatusVariant(status: WaitlistData["status"]) {
  switch (status) {
    case "notified":
      return "info" as const;
    case "seated":
      return "success" as const;
    case "expired":
      return "danger" as const;
    case "skipped":
      return "warning" as const;
    default:
      return "neutral" as const;
  }
}

function formatWaitlistSeating(entry: WaitlistData) {
  if (entry.tablesAvailableNow > 0 && entry.suggestedTable !== null) {
    return `Ready now: table ${entry.suggestedTable}`;
  }
  if (entry.tablesAvailableNow > 0) {
    return `${entry.tablesAvailableNow} table${entry.tablesAvailableNow === 1 ? "" : "s"} ready`;
  }
  if (entry.status === "expired") {
    return "Invite again when a table opens";
  }
  return "Waiting for a matching table";
}
