import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { StatCard } from "@/components/shared/StatCard";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { SectionHeader } from "@/components/shared/SectionHeader";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { FormDialog } from "@/components/shared/FormDialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Package, AlertTriangle, TrendingDown, Plus, ArrowUpDown, Search, Edit, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { apiFetch } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import { mapInventoryData } from "@/lib/api/mappers";
import type { InventoryOverviewResponse } from "@/lib/api/types";

export const Route = createFileRoute("/_app/inventory")({
  component: InventoryPage,
  head: () => ({ meta: [{ title: "Inventory Monitoring - IRMS" }] }),
});

interface IngredientData {
  id: string;
  name: string;
  unit: string;
  current: number;
  min: number;
  max: number;
  cost: number;
  category: string;
  lastRestock: string;
  affected: string[];
}

interface IngredientFormState {
  name: string;
  unit: string;
  current: string;
  min: string;
  max: string;
  cost: string;
  category: string;
}

function InventoryPage() {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState("");
  const [showAddIngredient, setShowAddIngredient] = useState(false);
  const [showEditIngredient, setShowEditIngredient] = useState<IngredientData | null>(null);
  const [showDeleteIngredient, setShowDeleteIngredient] = useState<IngredientData | null>(null);

  const [newIngredient, setNewIngredient] = useState<IngredientFormState>({
    name: "",
    unit: "pieces",
    current: "0",
    min: "5",
    max: "50",
    cost: "0",
    category: "Protein",
  });

  const { data, isLoading } = useQuery({
    queryKey: ["inventory"],
    queryFn: () => apiFetch<InventoryOverviewResponse>(ENDPOINTS.inventory.overview),
  });

  const inventory = data ? mapInventoryData(data) : null;
  const stockItems: IngredientData[] = (inventory?.items ?? []).map((item) => ({
    ...item,
    current: Number(item.current),
    min: Number(item.min),
    max: Number(item.max),
    cost: Number(item.cost),
  }));
  const transactions = (inventory?.transactions ?? []).map((transaction) => ({
    ...transaction,
    quantity: Number(transaction.quantity),
  }));
  const alerts = inventory?.alerts ?? [];
  const recommendations = (inventory?.recommendations ?? []).map((recommendation) => ({
    ...recommendation,
    threshold: Number(recommendation.threshold),
    targetQty: Number(recommendation.targetQty),
    onHand: Number(recommendation.onHand),
    averageUsage: Number(recommendation.averageUsage),
    projectedNeed: Number(recommendation.projectedNeed),
    recommendedOrderQty: Number(recommendation.recommendedOrderQty),
  }));

  const filtered = useMemo(
    () =>
      stockItems.filter((item) =>
        searchQuery === "" ? true : item.name.toLowerCase().includes(searchQuery.toLowerCase()),
      ),
    [searchQuery, stockItems],
  );

  const lowStockCount = stockItems.filter((item) => item.current <= item.min).length;
  const recentConsumption = transactions
    .filter((transaction) => transaction.type === "consumption")
    .reduce((total, transaction) => total + Math.abs(transaction.quantity), 0);

  const refreshInventory = async (message: string) => {
    await queryClient.invalidateQueries({ queryKey: ["inventory"] });
    toast.success(message);
  };

  const createMutation = useMutation({
    mutationFn: () =>
      apiFetch(ENDPOINTS.inventory.items, {
        method: "POST",
        body: JSON.stringify(toInventoryPayload(newIngredient)),
      }),
    onSuccess: async () => {
      setNewIngredient({
        name: "",
        unit: "pieces",
        current: "0",
        min: "5",
        max: "50",
        cost: "0",
        category: "Protein",
      });
      setShowAddIngredient(false);
      await refreshInventory("Ingredient added");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const updateMutation = useMutation({
    mutationFn: (ingredient: IngredientData) =>
      apiFetch(ENDPOINTS.inventory.item(ingredient.id), {
        method: "PUT",
        body: JSON.stringify({
          name: ingredient.name,
          unit: ingredient.unit,
          current: ingredient.current,
          minimum: ingredient.min,
          maximum: ingredient.max,
          cost: ingredient.cost,
          category: ingredient.category,
        }),
      }),
    onSuccess: async () => {
      setShowEditIngredient(null);
      await refreshInventory("Ingredient updated");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const deleteMutation = useMutation({
    mutationFn: (ingredientId: string) =>
      apiFetch(ENDPOINTS.inventory.item(ingredientId), {
        method: "DELETE",
      }),
    onSuccess: async () => {
      setShowDeleteIngredient(null);
      await refreshInventory("Ingredient deleted");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const acknowledgeMutation = useMutation({
    mutationFn: (alertId: string) =>
      apiFetch(ENDPOINTS.inventory.alertAcknowledge(alertId), {
        method: "PATCH",
      }),
    onSuccess: async () => {
      await refreshInventory("Alert acknowledged");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  return (
    <div className="space-y-6">
      <SectionHeader
        title="Inventory Monitoring"
        description="Kitchen activity updates stock levels quickly, and low-stock alerts stay visible during service."
        actions={(
          <Button onClick={() => setShowAddIngredient(true)}>
            <Plus className="w-4 h-4 mr-2" /> Add Ingredient
          </Button>
        )}
      />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <StatCard title="Total Ingredients" value={stockItems.length} subtitle="Tracked items" icon={Package} />
        <StatCard title="Low Stock Alerts" value={lowStockCount} subtitle="Below threshold" icon={AlertTriangle} variant="danger" />
        <StatCard
          title="Today's Consumption"
          value={`${recentConsumption} units`}
          subtitle="Across recent stock transactions"
          icon={TrendingDown}
          variant="warning"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-foreground flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-destructive" /> Low-Stock Alerts
            </h2>
            <span className="text-xs text-muted-foreground">{alerts.length} tracked</span>
          </div>
          <div className="space-y-2">
            {alerts.map((alert) => (
              <div key={alert.alertId} className="rounded-lg border border-border/60 bg-muted/30 p-3">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-medium text-foreground">{alert.itemName}</p>
                    <p className="text-[10px] text-muted-foreground">
                      Severity: {alert.severity} - Raised {alert.createdAt}
                    </p>
                    {alert.affectedDishes.length > 0 && (
                      <p className="text-[10px] text-muted-foreground mt-1">
                        Affects: {alert.affectedDishes.join(", ")}
                      </p>
                    )}
                  </div>
                  <StatusBadge
                    status={alert.status}
                    variant={alert.status === "open" ? "danger" : "neutral"}
                  />
                </div>
                <div className="flex items-center justify-between mt-3">
                  <span className="text-[10px] text-muted-foreground">
                    {alert.acknowledgedBy ? `Acknowledged by ${alert.acknowledgedBy} ${alert.acknowledgedAt ? `(${alert.acknowledgedAt})` : ""}` : "Awaiting review"}
                  </span>
                  {alert.status === "open" && (
                    <Button size="sm" variant="outline" className="h-7 text-[10px]" onClick={() => void acknowledgeMutation.mutate(alert.alertId)}>
                      Acknowledge
                    </Button>
                  )}
                </div>
              </div>
            ))}
            {alerts.length === 0 && (
              <div className="py-6 text-center text-xs text-muted-foreground">No low-stock alerts are active.</div>
            )}
          </div>
        </div>

        <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-foreground flex items-center gap-2">
              <TrendingDown className="w-4 h-4 text-warning" /> Reorder Recommendations
            </h2>
            <span className="text-xs text-muted-foreground">{recommendations.length} items</span>
          </div>
          <div className="space-y-2">
            {recommendations.map((recommendation) => (
              <div key={recommendation.inventoryItemId} className="rounded-lg border border-border/60 bg-muted/30 p-3">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-medium text-foreground">{recommendation.name}</p>
                    <p className="text-[10px] text-muted-foreground">
                      On hand {recommendation.onHand} / target {recommendation.targetQty} / threshold {recommendation.threshold}
                    </p>
                  </div>
                  <StatusBadge
                    status={recommendation.confidence === "manual_review" ? "manual review" : "calculated"}
                    variant={recommendation.confidence === "manual_review" ? "warning" : "success"}
                  />
                </div>
                <div className="mt-3 grid grid-cols-3 gap-2 text-[10px] text-muted-foreground">
                  <div>
                    <span className="block">Average usage</span>
                    <span className="text-foreground font-mono">{recommendation.averageUsage}</span>
                  </div>
                  <div>
                    <span className="block">Projected need</span>
                    <span className="text-foreground font-mono">{recommendation.projectedNeed}</span>
                  </div>
                  <div>
                    <span className="block">Recommended order</span>
                    <span className="text-foreground font-mono">{recommendation.recommendedOrderQty}</span>
                  </div>
                </div>
              </div>
            ))}
            {recommendations.length === 0 && (
              <div className="py-6 text-center text-xs text-muted-foreground">No reorder recommendations are available yet.</div>
            )}
          </div>
        </div>
      </div>

      <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-semibold text-foreground">Stock Ledger</h2>
          <div className="relative">
            <Search className="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
            <Input
              className="pl-8 w-56 h-8 text-sm"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="Search..."
            />
          </div>
        </div>
        {isLoading ? (
          <div className="py-10 text-center text-sm text-muted-foreground">Loading inventory...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  {["Ingredient", "Category", "Stock", "Min", "Status", "Cost/Unit", "Last Restock", "Actions"].map((heading) => (
                    <th
                      key={heading}
                      className="text-left py-2.5 px-2 text-xs font-medium text-muted-foreground uppercase tracking-wider"
                    >
                      {heading}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {filtered.map((item) => {
                  const isLow = item.current <= item.min;
                  return (
                    <tr key={item.id} className={`border-b border-border/30 hover:bg-muted/30 ${isLow ? "bg-red-50" : ""}`}>
                      <td className="py-3 px-2 text-sm font-medium text-foreground">{item.name}</td>
                      <td className="py-3 px-2 text-sm text-muted-foreground">{item.category}</td>
                      <td className="py-3 px-2">
                        <div className="flex items-center gap-2">
                          <div className="w-16 h-1.5 rounded-full bg-muted overflow-hidden">
                            <div
                              className={`h-full rounded-full ${
                                isLow ? "bg-red-500" : item.current / Math.max(item.max, 1) > 0.5 ? "bg-emerald-500" : "bg-amber-500"
                              }`}
                              style={{ width: `${Math.min(100, (item.current / Math.max(item.max, 1)) * 100)}%` }}
                            />
                          </div>
                          <span className={`text-sm font-mono ${isLow ? "text-red-600 font-bold" : "text-foreground"}`}>
                            {item.current} {item.unit}
                          </span>
                        </div>
                      </td>
                      <td className="py-3 px-2 text-sm text-muted-foreground font-mono">{item.min}</td>
                      <td className="py-3 px-2">
                        <StatusBadge status={isLow ? "Low Stock" : "OK"} variant={isLow ? "danger" : "success"} />
                      </td>
                      <td className="py-3 px-2 text-sm text-muted-foreground font-mono">${item.cost.toFixed(2)}</td>
                      <td className="py-3 px-2 text-sm text-muted-foreground">{item.lastRestock}</td>
                      <td className="py-3 px-2">
                        <div className="flex gap-1">
                          <Button size="sm" variant="ghost" className="h-7" onClick={() => setShowEditIngredient({ ...item })}>
                            <Edit className="w-3.5 h-3.5" />
                          </Button>
                          <Button
                            size="sm"
                            variant="ghost"
                            className="h-7 text-destructive"
                            onClick={() => setShowDeleteIngredient(item)}
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
        <h2 className="text-sm font-semibold text-foreground mb-4 flex items-center gap-2">
          <ArrowUpDown className="w-4 h-4 text-primary" /> Recent Stock Transactions
        </h2>
        <div className="space-y-2">
          {transactions.map((transaction) => (
            <div key={transaction.id} className="flex items-center justify-between p-3 rounded-lg bg-muted/30 border border-border/50">
              <div className="flex items-center gap-3">
                <StatusBadge
                  status={transaction.type}
                  variant={transaction.type === "restock" ? "success" : transaction.type === "waste" ? "danger" : "info"}
                />
                <div>
                  <span className="text-sm text-foreground">{transaction.item}</span>
                  <span className="text-xs text-muted-foreground ml-2">by {transaction.by}</span>
                </div>
              </div>
              <div className="flex items-center gap-4">
                <span
                  className={`text-sm font-mono font-medium ${
                    transaction.quantity > 0 ? "text-emerald-600" : "text-red-600"
                  }`}
                >
                  {transaction.quantity > 0 ? "+" : ""}
                  {transaction.quantity}
                </span>
                <span className="text-xs text-muted-foreground w-20 text-right">{transaction.time}</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      <FormDialog
        open={showAddIngredient}
        onOpenChange={setShowAddIngredient}
        title="Add Ingredient"
        onSubmit={() => void createMutation.mutate()}
        submitDisabled={!isInventoryFormValid(newIngredient)}
      >
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label>Ingredient Name *</Label>
            <Input value={newIngredient.name} onChange={(event) => setNewIngredient((current) => ({ ...current, name: event.target.value }))} />
          </div>
          <div className="space-y-2">
            <Label>Unit</Label>
            <Input value={newIngredient.unit} onChange={(event) => setNewIngredient((current) => ({ ...current, unit: event.target.value }))} placeholder="pieces, lbs, kegs..." />
          </div>
          <div className="space-y-2">
            <Label>Current Stock</Label>
            <Input type="number" value={newIngredient.current} onChange={(event) => setNewIngredient((current) => ({ ...current, current: event.target.value }))} />
          </div>
          <div className="space-y-2">
            <Label>Cost per Unit ($)</Label>
            <Input type="number" step="0.01" value={newIngredient.cost} onChange={(event) => setNewIngredient((current) => ({ ...current, cost: event.target.value }))} />
          </div>
          <div className="space-y-2">
            <Label>Low Stock Threshold</Label>
            <Input type="number" value={newIngredient.min} onChange={(event) => setNewIngredient((current) => ({ ...current, min: event.target.value }))} />
          </div>
          <div className="space-y-2">
            <Label>Max Stock</Label>
            <Input type="number" value={newIngredient.max} onChange={(event) => setNewIngredient((current) => ({ ...current, max: event.target.value }))} />
          </div>
          <div className="space-y-2 col-span-2">
            <Label>Category</Label>
            <select
              value={newIngredient.category}
              onChange={(event) => setNewIngredient((current) => ({ ...current, category: event.target.value }))}
              className="w-full px-3 py-2 rounded-md border border-border bg-background text-sm"
            >
              {["Protein", "Produce", "Dairy", "Beverages", "Dry Goods", "Spices"].map((category) => (
                <option key={category} value={category}>
                  {category}
                </option>
              ))}
            </select>
          </div>
        </div>
      </FormDialog>

      {showEditIngredient && (
        <FormDialog
          open={!!showEditIngredient}
          onOpenChange={() => setShowEditIngredient(null)}
          title="Edit Ingredient"
          onSubmit={() => void updateMutation.mutate(showEditIngredient)}
        >
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Ingredient Name</Label>
              <Input
                value={showEditIngredient.name}
                onChange={(event) =>
                  setShowEditIngredient((current) => (current ? { ...current, name: event.target.value } : null))
                }
              />
            </div>
            <div className="space-y-2">
              <Label>Unit</Label>
              <Input
                value={showEditIngredient.unit}
                onChange={(event) =>
                  setShowEditIngredient((current) => (current ? { ...current, unit: event.target.value } : null))
                }
              />
            </div>
            <div className="space-y-2">
              <Label>Current Stock</Label>
              <Input
                type="number"
                value={showEditIngredient.current}
                onChange={(event) =>
                  setShowEditIngredient((current) =>
                    current ? { ...current, current: Number(event.target.value) || 0 } : null,
                  )
                }
              />
            </div>
            <div className="space-y-2">
              <Label>Cost per Unit ($)</Label>
              <Input
                type="number"
                step="0.01"
                value={showEditIngredient.cost}
                onChange={(event) =>
                  setShowEditIngredient((current) =>
                    current ? { ...current, cost: Number(event.target.value) || 0 } : null,
                  )
                }
              />
            </div>
            <div className="space-y-2">
              <Label>Low Stock Threshold</Label>
              <Input
                type="number"
                value={showEditIngredient.min}
                onChange={(event) =>
                  setShowEditIngredient((current) =>
                    current ? { ...current, min: Number(event.target.value) || 0 } : null,
                  )
                }
              />
            </div>
            <div className="space-y-2">
              <Label>Max Stock</Label>
              <Input
                type="number"
                value={showEditIngredient.max}
                onChange={(event) =>
                  setShowEditIngredient((current) =>
                    current ? { ...current, max: Number(event.target.value) || 0 } : null,
                  )
                }
              />
            </div>
          </div>
        </FormDialog>
      )}

      <ConfirmDialog
        open={!!showDeleteIngredient}
        onOpenChange={() => setShowDeleteIngredient(null)}
        title="Delete Ingredient"
        description={`Delete "${showDeleteIngredient?.name}"? This cannot be undone.`}
        confirmLabel="Delete"
        variant="destructive"
        onConfirm={() => {
          if (showDeleteIngredient) {
            void deleteMutation.mutate(showDeleteIngredient.id);
          }
        }}
      />
    </div>
  );
}

function isInventoryFormValid(form: IngredientFormState) {
  return form.name.trim() !== "";
}

function toInventoryPayload(form: IngredientFormState) {
  return {
    name: form.name.trim(),
    unit: form.unit.trim(),
    current: Number(form.current),
    minimum: Number(form.min),
    maximum: Number(form.max),
    cost: Number(form.cost),
    category: form.category,
  };
}
