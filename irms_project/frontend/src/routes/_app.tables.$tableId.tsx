import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { CreditCard, ShoppingCart, Armchair } from "lucide-react";
import { apiFetch } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import { mapReservationData } from "@/lib/api/mappers";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { Button } from "@/components/ui/button";
import type { ReservationOverviewResponse } from "@/lib/api/types";

export const Route = createFileRoute("/_app/tables/$tableId")({
  component: TableDetailPage,
  head: () => ({ meta: [{ title: "Table Detail - IRMS" }] }),
});

function TableDetailPage() {
  const { tableId } = Route.useParams();
  const { data, isLoading } = useQuery({
    queryKey: ["reservations", "table-detail", tableId],
    queryFn: () => apiFetch<ReservationOverviewResponse>(ENDPOINTS.reservations.overview),
  });

  const overview = data ? mapReservationData(data) : null;
  const table = overview?.tables.find((entry) => entry.id === tableId) ?? null;

  if (isLoading) {
    return <div className="py-10 text-center text-sm text-muted-foreground">Loading table details...</div>;
  }

  if (!table) {
    return <div className="py-10 text-center text-sm text-muted-foreground">Table details are not available.</div>;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-heading font-bold text-foreground">Table #{table.number}</h1>
        <p className="text-xs text-muted-foreground mt-1">Review the current table status and jump into orders or billing with the right session.</p>
      </div>

      <div className="bg-card rounded-xl border border-border p-6 shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-3 rounded-xl bg-secondary">
              <Armchair className="w-5 h-5 text-primary" />
            </div>
            <div>
              <p className="text-sm font-semibold text-foreground">Table #{table.number}</p>
              <p className="text-xs text-muted-foreground">{table.capacity} seats</p>
            </div>
          </div>
          <StatusBadge status={table.status.replace("_", " ")} variant={table.status === "occupied" ? "warning" : "neutral"} />
        </div>

        <div className="grid gap-3 md:grid-cols-2">
          <DetailRow label="Zone" value={table.notes} />
          <DetailRow label="Check-in" value={table.checkinTime ?? "-"} />
          <DetailRow label="Reservation" value={table.reservationTime ?? "-"} />
          <DetailRow label="Session" value={table.sessionId ?? "-"} />
        </div>

        <div className="flex gap-3">
          {table.sessionId ? (
            <Button asChild>
              <Link to="/orders" search={{ sessionId: table.sessionId }}>
                <ShoppingCart className="w-4 h-4 mr-2" />
                Open Orders
              </Link>
            </Button>
          ) : (
            <Button disabled>
              <ShoppingCart className="w-4 h-4 mr-2" />
              Open Orders
            </Button>
          )}
          {table.sessionId ? (
            <Button asChild variant="outline">
              <Link to="/billing" search={{ sessionId: table.sessionId }}>
                <CreditCard className="w-4 h-4 mr-2" />
                Open Billing
              </Link>
            </Button>
          ) : (
            <Button variant="outline" disabled>
              <CreditCard className="w-4 h-4 mr-2" />
              Open Billing
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-border/60 bg-muted/30 px-4 py-3">
      <p className="text-[10px] uppercase tracking-wide text-muted-foreground">{label}</p>
      <p className="mt-1 text-sm text-foreground">{value}</p>
    </div>
  );
}
