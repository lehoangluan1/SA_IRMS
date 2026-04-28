import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { FormDialog } from "@/components/shared/FormDialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Users, Shield, Calendar, Plus, Search } from "lucide-react";
import { apiFetch } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import { mapStaffData } from "@/lib/api/mappers";
import type { StaffResponse } from "@/lib/api/types";
import { toast } from "sonner";

export const Route = createFileRoute("/_app/staff")({
  component: StaffPage,
  head: () => ({ meta: [{ title: "Staff & Roles - IRMS" }] }),
});

const roleColors: Record<string, "danger" | "warning" | "info" | "success" | "default" | "neutral"> = {
  admin: "danger",
  manager: "warning",
  server: "info",
  chef: "success",
  cashier: "default",
  host: "neutral",
};

function StaffPage() {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState("");
  const [shiftDate, setShiftDate] = useState(new Date().toISOString().slice(0, 10));
  const [showScheduleShift, setShowScheduleShift] = useState(false);
  const [showRoleEditor, setShowRoleEditor] = useState<StaffResponse["staff"][number] | null>(null);
  const [shiftForm, setShiftForm] = useState({
    userId: "",
    shiftDate: new Date().toISOString().slice(0, 10),
    startAt: "11:00",
    endAt: "19:00",
    position: "Server",
    zone: "Section A",
  });
  const [selectedRoleIds, setSelectedRoleIds] = useState<string[]>([]);

  const { data } = useQuery({
    queryKey: ["staff", searchQuery, shiftDate],
    queryFn: () => {
      const params = new URLSearchParams();
      if (searchQuery.trim() !== "") params.set("search", searchQuery.trim());
      if (shiftDate !== "") params.set("shiftDate", shiftDate);
      return apiFetch<StaffResponse>(`${ENDPOINTS.staff.overview}?${params.toString()}`);
    },
  });

  const scheduleMutation = useMutation({
    mutationFn: () =>
      apiFetch(ENDPOINTS.shifts, {
        method: "POST",
        body: JSON.stringify({
          userId: shiftForm.userId,
          shiftDate: shiftForm.shiftDate,
          startAt: shiftForm.startAt,
          endAt: shiftForm.endAt,
          position: shiftForm.position.trim(),
          zone: shiftForm.zone.trim() || null,
        }),
      }),
    onSuccess: async () => {
      setShowScheduleShift(false);
      await queryClient.invalidateQueries({ queryKey: ["staff"] });
      toast.success("Shift scheduled");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const roleMutation = useMutation({
    mutationFn: () =>
      apiFetch(showRoleEditor ? ENDPOINTS.staff.roles(showRoleEditor.userId) : "", {
        method: "PATCH",
        body: JSON.stringify({ roleIds: selectedRoleIds }),
      }),
    onSuccess: async () => {
      setShowRoleEditor(null);
      await queryClient.invalidateQueries({ queryKey: ["staff"] });
      toast.success("Roles updated. The selected user must sign in again.");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const mapped = data ? mapStaffData(data) : null;
  const staff = useMemo(() => mapped?.staff ?? [], [mapped]);
  const roles = useMemo(() => mapped?.roles ?? [], [mapped]);
  const shifts = useMemo(() => mapped?.shifts ?? [], [mapped]);

  const selectedStaff = useMemo(
    () => staff.find((member) => member.userId === shiftForm.userId) ?? staff[0] ?? null,
    [shiftForm.userId, staff],
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-heading font-bold text-foreground">Staff & Roles</h1>
          <p className="text-xs text-muted-foreground mt-1">
            Manage staff assignments, roles, and upcoming shifts.
          </p>
        </div>
        <Button
          onClick={() => {
            const firstStaffMember = staff[0];
            setShiftForm((current) => ({ ...current, userId: current.userId || firstStaffMember?.userId || "" }));
            setShowScheduleShift(true);
          }}
        >
          <Plus className="w-4 h-4 mr-2" /> Schedule Shift
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-card rounded-xl border border-border p-5">
          <div className="flex items-center justify-between mb-4 gap-3">
            <h2 className="text-sm font-medium text-foreground flex items-center gap-2">
              <Users className="w-4 h-4 text-primary" /> Staff Directory
            </h2>
            <div className="relative">
              <Search className="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
              <Input
                className="pl-8 w-56 h-8 text-sm"
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                placeholder="Search staff..."
              />
            </div>
          </div>
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                {["Name", "Email", "Roles", "Status", "Hired", "Actions"].map((header) => (
                  <th key={header} className="text-left py-2 px-2 text-[10px] font-medium text-muted-foreground uppercase tracking-wider">
                    {header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {staff.map((member) => (
                <tr key={member.userId} className="border-b border-border/30 hover:bg-secondary/20">
                  <td className="py-2.5 px-2 text-xs font-medium text-foreground">{member.displayName}</td>
                  <td className="py-2.5 px-2 text-xs text-muted-foreground">{member.email}</td>
                  <td className="py-2.5 px-2">
                    <div className="flex flex-wrap gap-1">
                      {member.roles.map((role) => (
                        <StatusBadge key={role} status={role} variant={roleColors[role] ?? "neutral"} />
                      ))}
                    </div>
                  </td>
                  <td className="py-2.5 px-2">
                    <StatusBadge status={member.status === "active" ? "Active" : "Inactive"} variant={member.status === "active" ? "success" : "neutral"} />
                  </td>
                  <td className="py-2.5 px-2 text-xs text-muted-foreground">{member.hireDate}</td>
                  <td className="py-2.5 px-2">
                    <Button
                      size="sm"
                      variant="ghost"
                      className="h-7 text-[10px]"
                      onClick={() => {
                        setShowRoleEditor(member);
                        const matchedRoleIds = roles.filter((role) => member.roles.includes(role.name)).map((role) => role.roleId);
                        setSelectedRoleIds(matchedRoleIds);
                      }}
                    >
                      Roles
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="space-y-4">
          <div className="bg-card rounded-xl border border-border p-5">
            <h2 className="text-sm font-medium text-foreground mb-3 flex items-center gap-2">
              <Shield className="w-4 h-4 text-destructive" /> Sensitive Permissions
            </h2>
            <div className="space-y-3">
              {roles.map((role) => (
                <div key={role.roleId}>
                  <p className="text-[10px] font-medium text-muted-foreground uppercase tracking-wider mb-1">
                    {role.name} · {role.scope}
                  </p>
                  <div className="flex flex-wrap gap-1">
                    {role.permissions.map((permission) => (
                      <span key={permission} className="px-2 py-0.5 rounded text-[9px] bg-destructive/10 text-destructive font-mono">{permission}</span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
            <p className="text-[9px] text-muted-foreground mt-3">All sensitive actions are recorded in the audit log with actor, reason, and correlation ID.</p>
          </div>

          <div className="bg-card rounded-xl border border-border p-5">
            <div className="flex items-center justify-between mb-3 gap-3">
              <h2 className="text-sm font-medium text-foreground flex items-center gap-2">
                <Calendar className="w-4 h-4 text-info" /> Shift Schedule
              </h2>
              <Input type="date" value={shiftDate} onChange={(event) => setShiftDate(event.target.value)} className="w-40 h-8 text-sm" />
            </div>
            <div className="space-y-2">
              {shifts.map((shift) => (
                <div key={shift.shiftAssignmentId} className="flex items-center justify-between p-2 rounded-lg bg-secondary/30">
                  <div>
                    <p className="text-xs font-medium text-foreground">{shift.displayName}</p>
                    <p className="text-[10px] text-muted-foreground">{shift.position} - {shift.zone ?? shift.roleName}</p>
                  </div>
                  <span className="text-[10px] font-mono text-muted-foreground">{shift.startAt}-{shift.endAt}</span>
                </div>
              ))}
              {shifts.length === 0 && (
                <div className="py-6 text-center text-xs text-muted-foreground">No shifts are scheduled for the selected date.</div>
              )}
            </div>
          </div>
        </div>
      </div>

      <FormDialog
        open={showScheduleShift}
        onOpenChange={setShowScheduleShift}
        title="Schedule Shift"
        description="Choose the staff member, date, and zone for the assignment."
        submitLabel="Save Shift"
        onSubmit={() => void scheduleMutation.mutate()}
        submitDisabled={!shiftForm.userId || !shiftForm.shiftDate || !shiftForm.position.trim()}
      >
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2 col-span-2">
            <Label>Staff Member</Label>
            <select
              value={shiftForm.userId}
              onChange={(event) => setShiftForm((current) => ({ ...current, userId: event.target.value }))}
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm"
            >
              <option value="">Select staff member</option>
              {staff.map((member) => (
                <option key={member.userId} value={member.userId}>
                  {member.displayName}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-2">
            <Label>Date</Label>
            <Input type="date" value={shiftForm.shiftDate} onChange={(event) => setShiftForm((current) => ({ ...current, shiftDate: event.target.value }))} />
          </div>
          <div className="space-y-2">
            <Label>Position</Label>
            <Input value={shiftForm.position} onChange={(event) => setShiftForm((current) => ({ ...current, position: event.target.value }))} />
          </div>
          <div className="space-y-2">
            <Label>Start</Label>
            <Input type="time" value={shiftForm.startAt} onChange={(event) => setShiftForm((current) => ({ ...current, startAt: event.target.value }))} />
          </div>
          <div className="space-y-2">
            <Label>End</Label>
            <Input type="time" value={shiftForm.endAt} onChange={(event) => setShiftForm((current) => ({ ...current, endAt: event.target.value }))} />
          </div>
          <div className="space-y-2 col-span-2">
            <Label>Zone</Label>
            <Input value={shiftForm.zone} onChange={(event) => setShiftForm((current) => ({ ...current, zone: event.target.value }))} placeholder="Section A" />
          </div>
          {selectedStaff && (
            <div className="col-span-2 rounded-lg bg-secondary/30 p-3 text-xs text-muted-foreground">
              Selected role scope: {selectedStaff.roles.join(", ") || "No roles assigned"}
            </div>
          )}
        </div>
      </FormDialog>

      {showRoleEditor && (
        <FormDialog
          open={!!showRoleEditor}
          onOpenChange={() => setShowRoleEditor(null)}
          title={`Update Roles - ${showRoleEditor.displayName}`}
          description="Role changes are audited and force the selected user to sign in again."
          submitLabel="Save Roles"
          onSubmit={() => void roleMutation.mutate()}
          submitDisabled={selectedRoleIds.length === 0}
        >
          <div className="space-y-3">
            {roles.map((role) => {
              const checked = selectedRoleIds.includes(role.roleId);
              return (
                <label key={role.roleId} className="flex items-start gap-3 rounded-lg border border-border p-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={checked}
                    onChange={(event) =>
                      setSelectedRoleIds((current) =>
                        event.target.checked ? [...current, role.roleId] : current.filter((roleId) => roleId !== role.roleId),
                      )
                    }
                    className="mt-0.5 rounded border-border"
                  />
                  <div>
                    <p className="text-sm font-medium text-foreground">{role.name}</p>
                    <p className="text-xs text-muted-foreground">{role.description}</p>
                    <p className="text-[10px] text-muted-foreground mt-1">Scope: {role.scope}</p>
                  </div>
                </label>
              );
            })}
          </div>
        </FormDialog>
      )}
    </div>
  );
}
