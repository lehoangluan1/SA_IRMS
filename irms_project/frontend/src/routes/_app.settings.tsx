import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { Bell, Clock, Globe, Settings as SettingsIcon } from "lucide-react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { apiFetch } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import { mapSettingsData } from "@/lib/api/mappers";
import { useAuth } from "@/lib/auth";
import type { SettingsResponse } from "@/lib/api/types";

export const Route = createFileRoute("/_app/settings")({
  component: SettingsPage,
  head: () => ({ meta: [{ title: "Settings - IRMS" }] }),
});

function SettingsPage() {
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const { data, isLoading } = useQuery({
    queryKey: ["settings"],
    queryFn: () => apiFetch<SettingsResponse>(ENDPOINTS.settings),
  });

  const settings = data ? mapSettingsData(data) : null;
  const operations = useMemo(() => (settings?.operations ?? {}) as Record<string, unknown>, [settings]);
  const general = useMemo(() => (settings?.general ?? {}) as Record<string, unknown>, [settings]);
  const sessionSecurity = useMemo(() => (settings?.sessionSecurity ?? {}) as Record<string, unknown>, [settings]);
  const canManageSettings = Boolean(user?.permissions.includes("all") || user?.permissions.includes("settings.manage"));

  const [form, setForm] = useState({
    waitlistHoldMinutes: "",
    reservationGraceMinutes: "",
    kitchenRushThresholdMinutes: "",
    kitchenLateThresholdMinutes: "",
    refundWindowHours: "",
  });

  useEffect(() => {
    if (!settings) return;
    setForm({
      waitlistHoldMinutes: String(operations.waitlistHoldMinutes ?? ""),
      reservationGraceMinutes: String(operations.reservationGraceMinutes ?? ""),
      kitchenRushThresholdMinutes: String(operations.kitchenRushThresholdMinutes ?? ""),
      kitchenLateThresholdMinutes: String(operations.kitchenLateThresholdMinutes ?? ""),
      refundWindowHours: String(operations.refundWindowHours ?? ""),
    });
  }, [operations, settings]);

  const saveMutation = useMutation({
    mutationFn: () =>
      apiFetch<SettingsResponse>(ENDPOINTS.settings, {
        method: "PATCH",
        body: JSON.stringify({
          waitlistHoldMinutes: Number(form.waitlistHoldMinutes),
          reservationGraceMinutes: Number(form.reservationGraceMinutes),
          kitchenRushThresholdMinutes: Number(form.kitchenRushThresholdMinutes),
          kitchenLateThresholdMinutes: Number(form.kitchenLateThresholdMinutes),
          refundWindowHours: Number(form.refundWindowHours),
        }),
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["settings"] });
      toast.success("Settings saved");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-heading font-bold text-foreground">Settings</h1>
        <p className="text-xs text-muted-foreground mt-1">
          Adjust restaurant operating rules used by reservations, kitchen, billing, and alerts.
        </p>
      </div>

      <div className="grid gap-4 xl:grid-cols-3">
        <div className="bg-card rounded-xl border border-border p-5">
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2 rounded-lg bg-secondary">
              <Globe className="w-4 h-4 text-primary" />
            </div>
            <div>
              <h2 className="text-sm font-medium text-foreground">General</h2>
              <p className="text-[10px] text-muted-foreground">Restaurant profile</p>
            </div>
          </div>
          <SettingRow label="Restaurant Name" value={general.restaurantName} />
          <SettingRow label="Timezone" value={general.timezone} />
          <SettingRow label="Currency" value={general.currency} />
          <SettingRow label="Tax Rate" value={formatSettingValue("taxRate", general.taxRate)} />
          <SettingRow label="Service Fee" value={formatSettingValue("serviceFeeRate", general.serviceFeeRate)} />
        </div>

        <div className="bg-card rounded-xl border border-border p-5">
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2 rounded-lg bg-secondary">
              <Clock className="w-4 h-4 text-primary" />
            </div>
            <div>
              <h2 className="text-sm font-medium text-foreground">Session Security</h2>
              <p className="text-[10px] text-muted-foreground">Current sign-in time limits</p>
            </div>
          </div>
          <SettingRow label="Idle Timeout" value={formatSettingValue("idleTimeoutMinutes", sessionSecurity.idleTimeoutMinutes)} />
          <SettingRow label="Session Timeout" value={formatSettingValue("sessionTimeoutHours", sessionSecurity.sessionTimeoutHours)} />
        </div>

        <div className="bg-card rounded-xl border border-border p-5">
          <div className="flex items-center gap-3 mb-4">
            <div className="p-2 rounded-lg bg-secondary">
              <Bell className="w-4 h-4 text-primary" />
            </div>
            <div>
              <h2 className="text-sm font-medium text-foreground">Operations</h2>
              <p className="text-[10px] text-muted-foreground">These values apply immediately to live workflows.</p>
            </div>
          </div>

          {isLoading ? (
            <div className="py-3 px-3 rounded-lg bg-secondary/30 text-xs text-muted-foreground">
              Loading settings...
            </div>
          ) : (
            <div className="space-y-3">
              <EditableSetting
                label="Waitlist Response Window"
                suffix="minutes"
                value={form.waitlistHoldMinutes}
                disabled={!canManageSettings || saveMutation.isPending}
                onChange={(value) => setForm((current) => ({ ...current, waitlistHoldMinutes: value }))}
              />
              <EditableSetting
                label="Reservation Arrival Grace Period"
                suffix="minutes"
                value={form.reservationGraceMinutes}
                disabled={!canManageSettings || saveMutation.isPending}
                onChange={(value) => setForm((current) => ({ ...current, reservationGraceMinutes: value }))}
              />
              <EditableSetting
                label="Rush Kitchen Alert"
                suffix="minutes"
                value={form.kitchenRushThresholdMinutes}
                disabled={!canManageSettings || saveMutation.isPending}
                onChange={(value) => setForm((current) => ({ ...current, kitchenRushThresholdMinutes: value }))}
              />
              <EditableSetting
                label="Late Kitchen Alert"
                suffix="minutes"
                value={form.kitchenLateThresholdMinutes}
                disabled={!canManageSettings || saveMutation.isPending}
                onChange={(value) => setForm((current) => ({ ...current, kitchenLateThresholdMinutes: value }))}
              />
              <EditableSetting
                label="Refund Approval Window"
                suffix="hours"
                value={form.refundWindowHours}
                disabled={!canManageSettings || saveMutation.isPending}
                onChange={(value) => setForm((current) => ({ ...current, refundWindowHours: value }))}
              />
              <Button className="w-full" disabled={!canManageSettings || saveMutation.isPending} onClick={() => void saveMutation.mutate()}>
                <SettingsIcon className="w-4 h-4 mr-2" />
                {saveMutation.isPending ? "Saving..." : "Save Settings"}
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function SettingRow({ label, value }: { label: string; value: unknown }) {
  return (
    <div className="flex items-center justify-between py-2.5 px-3 rounded-lg hover:bg-secondary/30 transition-colors">
      <span className="text-xs text-muted-foreground">{label}</span>
      <span className="text-xs text-foreground font-medium">{String(value ?? "Not specified")}</span>
    </div>
  );
}

function EditableSetting({
  label,
  value,
  suffix,
  disabled,
  onChange,
}: {
  label: string;
  value: string;
  suffix: string;
  disabled: boolean;
  onChange: (value: string) => void;
}) {
  return (
    <div className="space-y-2">
      <Label>{label}</Label>
      <div className="flex items-center gap-2">
        <Input type="number" min={0} value={value} disabled={disabled} onChange={(event) => onChange(event.target.value)} />
        <span className="text-xs text-muted-foreground whitespace-nowrap">{suffix}</span>
      </div>
    </div>
  );
}

function formatSettingValue(key: string, value: unknown) {
  if (value === null || value === undefined || value === "") {
    return "Not specified";
  }

  if (typeof value === "number") {
    if (key.toLowerCase().includes("rate")) return `${value}%`;
    if (key.toLowerCase().includes("minutes")) return `${value} minutes`;
    if (key.toLowerCase().includes("hours")) return `${value} hours`;
  }

  return String(value);
}
