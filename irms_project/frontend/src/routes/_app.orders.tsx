import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { SectionHeader } from "@/components/shared/SectionHeader";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  AlertTriangle, Ban, Check, ClipboardList, Clock, Minus, Package2, Plus, Search, Send, ShoppingCart, X,
} from "lucide-react";
import { toast } from "sonner";
import { apiFetch } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import { mapOrdersData } from "@/lib/api/mappers";
import type { OrdersOverviewResponse } from "@/lib/api/types";

export const Route = createFileRoute("/_app/orders")({
  validateSearch: (search: Record<string, unknown>) => ({
    sessionId: typeof search.sessionId === "string" ? search.sessionId : undefined,
  }),
  component: OrdersPage,
  head: () => ({ meta: [{ title: "Orders - IRMS" }] }),
});

interface CartItem {
  menuItemId: string;
  name: string;
  price: number;
  quantity: number;
  allergens: string[];
  note: string;
  station: string;
  modifierGroups: OrdersOverviewResponse["menuItems"][number]["modifierGroups"];
  modifierOptionIds: string[];
  sendLater: boolean;
}

interface CartCombo {
  comboId: string;
  name: string;
  description: string;
  price: number;
  quantity: number;
  allergyNotes: string;
  specialInstructions: string;
  groups: OrdersOverviewResponse["combos"][number]["groups"];
  selectedOptionsByGroup: Record<string, string[]>;
}

function OrdersPage() {
  const navigate = useNavigate();
  const { sessionId } = Route.useSearch();
  const queryClient = useQueryClient();
  const [selectedCategory, setSelectedCategory] = useState("All");
  const [cart, setCart] = useState<CartItem[]>([]);
  const [comboCart, setComboCart] = useState<CartCombo[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(sessionId ?? null);
  const [showCancelItemDialog, setShowCancelItemDialog] = useState<string | null>(null);
  const [showCancelOrderDialog, setShowCancelOrderDialog] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ["orders", selectedSessionId ?? "default"],
    queryFn: () => {
      const path = selectedSessionId
        ? `${ENDPOINTS.orders.overview}?sessionId=${selectedSessionId}`
        : ENDPOINTS.orders.overview;
      return apiFetch<OrdersOverviewResponse>(path);
    },
  });

  const overview = data ? mapOrdersData(data) : null;
  const sessions = useMemo(() => overview?.sessions ?? [], [overview]);
  const menuItems = (overview?.menuItems ?? []).map((item) => ({
    ...item,
    price: Number(item.price),
  }));
  const combos = (overview?.combos ?? []).map((combo) => ({
    ...combo,
    price: Number(combo.price),
    groups: combo.groups.map((group) => ({
      ...group,
      options: group.options.map((option) => ({
        ...option,
        extraPrice: Number(option.extraPrice),
      })),
    })),
  }));
  const orderedItems = (overview?.orderedItems ?? []).map((item) => ({
    ...item,
    price: Number(item.price),
    status: normalizeOrderStatus(item.status, item.orderStatus),
  }));

  useEffect(() => {
    if (!selectedSessionId) {
      setSelectedSessionId((overview?.selectedSessionId as string | null) ?? sessions[0]?.sessionId ?? null);
    }
  }, [overview?.selectedSessionId, selectedSessionId, sessions]);

  useEffect(() => {
    if (sessionId && sessionId !== selectedSessionId) {
      setSelectedSessionId(sessionId);
    }
  }, [selectedSessionId, sessionId]);

  const selectedSession = sessions.find((session) => session.sessionId === selectedSessionId) ?? sessions[0] ?? null;
  const categories = useMemo(() => ["All", ...new Set(menuItems.map((item) => item.category))], [menuItems]);
  const filteredItems = useMemo(
    () => menuItems.filter((item) => (
      (selectedCategory === "All" || item.category === selectedCategory)
      && (searchQuery === "" || item.name.toLowerCase().includes(searchQuery.toLowerCase()))
    )),
    [menuItems, searchQuery, selectedCategory],
  );
  const filteredCombos = useMemo(
    () => combos.filter((combo) => (
      combo.active
      && (searchQuery === "" || `${combo.name} ${combo.description}`.toLowerCase().includes(searchQuery.toLowerCase()))
    )),
    [combos, searchQuery],
  );

  const selectedModifierTotal = (item: CartItem) => item.modifierGroups
    .flatMap((group) => group.options)
    .filter((option) => item.modifierOptionIds.includes(option.id))
    .reduce((sum, option) => sum + Number(option.extraPrice), 0);

  const isModifierRequirementMissing = (item: CartItem) => item.modifierGroups.some((group) => {
    const selectedCount = group.options.filter((option) => item.modifierOptionIds.includes(option.id)).length;
    return (group.required || group.minSelect > 0) && selectedCount < group.minSelect;
  });

  const selectedComboExtraTotal = (combo: CartCombo) => combo.groups.reduce((sum, group) => {
    const selectedIds = new Set(combo.selectedOptionsByGroup[group.id] ?? []);
    return sum + group.options
      .filter((option) => selectedIds.has(option.id))
      .reduce((groupSum, option) => groupSum + Number(option.extraPrice), 0);
  }, 0);

  const isComboRequirementMissing = (combo: CartCombo) => combo.groups.some((group) => {
    const selectedCount = (combo.selectedOptionsByGroup[group.id] ?? []).length;
    return (group.required || group.minSelections > 0) && selectedCount < group.minSelections;
  });

  const total = cart.reduce((sum, item) => sum + (item.price + selectedModifierTotal(item)) * item.quantity, 0)
    + comboCart.reduce((sum, combo) => sum + (combo.price + selectedComboExtraTotal(combo)) * combo.quantity, 0);
  const hasMissingSelections = cart.some(isModifierRequirementMissing) || comboCart.some(isComboRequirementMissing);

  const refreshOrders = async (message: string) => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["orders"] }),
      queryClient.invalidateQueries({ queryKey: ["kitchen"] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
      queryClient.invalidateQueries({ queryKey: ["billing"] }),
      queryClient.invalidateQueries({ queryKey: ["reports"] }),
    ]);
    toast.success(message);
  };

  const createOrderMutation = useMutation({
    mutationFn: (draft: boolean) => apiFetch(ENDPOINTS.orders.create, {
      method: "POST",
      body: JSON.stringify({
        sessionId: selectedSession?.sessionId,
        specialInstructions: null,
        items: cart.map((item) => ({
          menuItemId: item.menuItemId,
          quantity: item.quantity,
          note: item.note || null,
          modifierOptionIds: item.modifierOptionIds,
          sendLater: item.sendLater,
        })),
        comboSelections: comboCart.map((combo) => ({
          comboId: combo.comboId,
          quantity: combo.quantity,
          allergyNotes: combo.allergyNotes.trim() || null,
          specialInstructions: combo.specialInstructions.trim() || null,
          groups: combo.groups.map((group) => ({
            comboGroupId: group.id,
            selectedOptionIds: combo.selectedOptionsByGroup[group.id] ?? [],
          })),
        })),
        draft,
      }),
    }),
    onSuccess: async (_, draft) => {
      setCart([]);
      setComboCart([]);
      await refreshOrders(draft ? "Draft order saved" : "Order sent to kitchen");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const actionMutation = useMutation({
    mutationFn: async (request: { path: string; method?: "PATCH" | "POST"; body?: unknown; successMessage: string }) => apiFetch(
      request.path,
      {
        method: request.method ?? "PATCH",
        body: request.body === undefined ? undefined : JSON.stringify(request.body),
      },
    ),
    onSuccess: async (_, variables) => {
      await refreshOrders(variables.successMessage);
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const addToCart = (item: (typeof menuItems)[number]) => {
    setCart((current) => {
      const existing = current.find((cartItem) => cartItem.menuItemId === item.id);
      if (existing) {
        return current.map((cartItem) => (
          cartItem.menuItemId === item.id ? { ...cartItem, quantity: cartItem.quantity + 1 } : cartItem
        ));
      }
      return [
        ...current,
        {
          menuItemId: item.id,
          name: item.name,
          price: item.price,
          quantity: 1,
          allergens: item.allergens,
          note: "",
          station: item.station,
          modifierGroups: item.modifierGroups ?? [],
          modifierOptionIds: [],
          sendLater: false,
        },
      ];
    });
  };

  const addComboToCart = (combo: (typeof combos)[number]) => {
    setComboCart((current) => {
      const existing = current.find((entry) => entry.comboId === combo.id);
      if (existing) {
        return current.map((entry) => (
          entry.comboId === combo.id ? { ...entry, quantity: entry.quantity + 1 } : entry
        ));
      }
      return [
        ...current,
        {
          comboId: combo.id,
          name: combo.name,
          description: combo.description,
          price: combo.price,
          quantity: 1,
          allergyNotes: "",
          specialInstructions: "",
          groups: combo.groups,
          selectedOptionsByGroup: {},
        },
      ];
    });
  };

  const updateQty = (id: string, delta: number) => {
    setCart((current) => current
      .map((item) => (item.menuItemId === id ? { ...item, quantity: Math.max(0, item.quantity + delta) } : item))
      .filter((item) => item.quantity > 0));
  };

  const updateComboQty = (id: string, delta: number) => {
    setComboCart((current) => current
      .map((combo) => (combo.comboId === id ? { ...combo, quantity: Math.max(0, combo.quantity + delta) } : combo))
      .filter((combo) => combo.quantity > 0));
  };

  const updateNote = (id: string, note: string) => {
    setCart((current) => current.map((item) => (item.menuItemId === id ? { ...item, note } : item)));
  };

  const updateComboField = (id: string, field: "allergyNotes" | "specialInstructions", value: string) => {
    setComboCart((current) => current.map((combo) => (
      combo.comboId === id ? { ...combo, [field]: value } : combo
    )));
  };

  const toggleSendLater = (id: string) => {
    setCart((current) => current.map((item) => (item.menuItemId === id ? { ...item, sendLater: !item.sendLater } : item)));
  };

  const toggleModifier = (id: string, groupId: string, optionId: string) => {
    setCart((current) => current.map((item) => {
      if (item.menuItemId !== id) return item;
      const group = item.modifierGroups.find((candidate) => candidate.id === groupId);
      const option = group?.options.find((candidate) => candidate.id === optionId);
      if (!group || !option?.active) return item;
      const currentSelections = new Set(item.modifierOptionIds);
      if (currentSelections.has(optionId)) {
        currentSelections.delete(optionId);
        return { ...item, modifierOptionIds: Array.from(currentSelections) };
      }
      const groupOptionIds = new Set(group.options.map((candidate) => candidate.id));
      const selectedInGroup = item.modifierOptionIds.filter((selectedId) => groupOptionIds.has(selectedId));
      if (!group.multiSelect) {
        groupOptionIds.forEach((selectedId) => currentSelections.delete(selectedId));
      } else if (selectedInGroup.length >= group.maxSelect) {
        toast.error(`${group.name} allows at most ${group.maxSelect} selection(s).`);
        return item;
      }
      currentSelections.add(optionId);
      return { ...item, modifierOptionIds: Array.from(currentSelections) };
    }));
  };

  const toggleComboOption = (comboId: string, groupId: string, optionId: string) => {
    setComboCart((current) => current.map((combo) => {
      if (combo.comboId !== comboId) return combo;
      const group = combo.groups.find((candidate) => candidate.id === groupId);
      const option = group?.options.find((candidate) => candidate.id === optionId);
      if (!group || !option?.active) return combo;
      const currentSelections = combo.selectedOptionsByGroup[groupId] ?? [];
      const selected = new Set(currentSelections);
      if (selected.has(optionId)) {
        selected.delete(optionId);
        return {
          ...combo,
          selectedOptionsByGroup: {
            ...combo.selectedOptionsByGroup,
            [groupId]: Array.from(selected),
          },
        };
      }
      if (currentSelections.length >= group.maxSelections) {
        if (group.maxSelections === 1) {
          return {
            ...combo,
            selectedOptionsByGroup: {
              ...combo.selectedOptionsByGroup,
              [groupId]: [optionId],
            },
          };
        }
        toast.error(`${group.name} allows at most ${group.maxSelections} option(s).`);
        return combo;
      }
      selected.add(optionId);
      return {
        ...combo,
        selectedOptionsByGroup: {
          ...combo.selectedOptionsByGroup,
          [groupId]: Array.from(selected),
        },
      };
    }));
  };

  const orderStatusVariant = (status: string) => {
    switch (status) {
      case "served":
        return "success" as const;
      case "draft":
        return "warning" as const;
      case "draft delayed":
        return "neutral" as const;
      case "ready":
        return "info" as const;
      case "cooking":
        return "warning" as const;
      case "delayed":
        return "danger" as const;
      case "pending":
        return "neutral" as const;
      case "cancelled":
        return "neutral" as const;
      default:
        return "neutral" as const;
    }
  };

  return (
    <div className="space-y-6">
      <SectionHeader
        title="Server POS - Orders"
        description="Create table orders, add combo selections, send dishes to the kitchen, and track service timing."
      />

      <div className="bg-card rounded-xl border border-border p-4 shadow-sm">
        <div className="flex items-center gap-4 flex-wrap">
          <div className="flex items-center gap-2">
            <Label className="text-sm text-muted-foreground whitespace-nowrap">Table:</Label>
            <select
              value={selectedSession?.sessionId ?? ""}
              onChange={(event) => {
                const nextSessionId = event.target.value || null;
                setSelectedSessionId(nextSessionId);
                void navigate({ to: "/orders", search: nextSessionId ? { sessionId: nextSessionId } : {} });
              }}
              className="px-3 py-1.5 rounded-lg border border-border text-sm bg-background focus:outline-none focus:ring-1 focus:ring-ring"
              disabled={sessions.length === 0}
            >
              {sessions.length === 0 ? (
                <option value="">No active tables</option>
              ) : sessions.map((session) => (
                <option key={session.sessionId} value={session.sessionId}>
                  Table #{session.tableNumber} ({session.guests} guests)
                </option>
              ))}
            </select>
          </div>
          <span className="text-sm text-muted-foreground">
            Session: <span className="font-mono text-foreground">{selectedSession?.sessionId ?? "-"}</span>
          </span>
          <span className="text-sm text-muted-foreground">
            Guests: <span className="font-medium text-foreground">{selectedSession?.guests ?? 0}</span>
          </span>
          <span className="text-sm text-muted-foreground">
            Combos: <span className="font-medium text-foreground">{combos.length}</span>
          </span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="relative flex-1">
                <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" />
                <Input value={searchQuery} onChange={(event) => setSearchQuery(event.target.value)} className="pl-9" placeholder="Search menu or combo..." />
              </div>
            </div>

            <div className="flex gap-1.5 overflow-x-auto pb-1">
              {categories.map((category) => (
                <button
                  key={category}
                  onClick={() => setSelectedCategory(category)}
                  className={`px-3 py-1.5 rounded-lg text-sm whitespace-nowrap transition-colors ${selectedCategory === category ? "bg-primary text-primary-foreground" : "bg-secondary text-muted-foreground hover:text-foreground"}`}
                >
                  {category}
                </button>
              ))}
            </div>

            <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-sm font-semibold text-foreground">Menu Items</h2>
                <span className="text-xs text-muted-foreground">{filteredItems.length} available</span>
              </div>
              {isLoading ? (
                <div className="py-10 text-center text-sm text-muted-foreground">Loading menu...</div>
              ) : (
                <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
                  {filteredItems.map((item) => (
                    <button
                      key={item.id}
                      disabled={!item.available || !selectedSession}
                      onClick={() => addToCart(item)}
                      className={`p-4 rounded-xl border text-left transition-all shadow-sm ${item.available && selectedSession ? "bg-card border-border hover:border-primary/50 hover:shadow-md" : "bg-muted border-border/50 opacity-50 cursor-not-allowed"}`}
                    >
                      <p className="text-sm font-medium text-foreground">{item.name}</p>
                      <p className="text-xs text-muted-foreground mt-0.5 capitalize">{item.station}</p>
                      <div className="flex items-center justify-between mt-2">
                        <span className="text-sm font-heading font-bold text-primary">${item.price.toFixed(2)}</span>
                        {!item.available && <StatusBadge status="Unavailable" variant="danger" />}
                      </div>
                      {item.allergens.length > 0 && (
                        <div className="flex items-center gap-1 mt-2">
                          <AlertTriangle className="w-3 h-3 text-amber-500 shrink-0" />
                          <span className="text-[10px] text-amber-600">{item.allergens.join(", ")}</span>
                        </div>
                      )}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>

          <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-semibold text-foreground flex items-center gap-2">
                <Package2 className="w-4 h-4 text-primary" />
                Menu Combos
              </h2>
              <span className="text-xs text-muted-foreground">{filteredCombos.length} active combos</span>
            </div>
            {filteredCombos.length === 0 ? (
              <div className="py-10 text-center text-sm text-muted-foreground">No active combos match the current search.</div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {filteredCombos.map((combo) => (
                  <button
                    key={combo.id}
                    type="button"
                    disabled={!selectedSession}
                    onClick={() => addComboToCart(combo)}
                    className={`p-4 rounded-xl border text-left transition-all shadow-sm ${selectedSession ? "bg-card border-border hover:border-primary/50 hover:shadow-md" : "bg-muted border-border/50 opacity-50 cursor-not-allowed"}`}
                  >
                    <div className="flex items-center justify-between gap-3">
                      <div>
                        <p className="text-sm font-medium text-foreground">{combo.name}</p>
                        <p className="text-xs text-muted-foreground mt-0.5">{combo.description}</p>
                      </div>
                      <span className="text-sm font-heading font-bold text-primary">${combo.price.toFixed(2)}</span>
                    </div>
                    <div className="mt-3 space-y-1">
                      {combo.groups.map((group) => (
                        <div key={group.id} className="text-[11px] text-muted-foreground">
                          {group.name}: {group.minSelections}-{group.maxSelections} selection(s)
                        </div>
                      ))}
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="space-y-4">
          <div className="bg-card rounded-xl border border-border p-5 shadow-sm sticky top-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-semibold text-foreground flex items-center gap-2">
                <ShoppingCart className="w-4 h-4 text-primary" />
                Current Order
              </h2>
              <span className="text-xs text-muted-foreground">Table #{selectedSession?.tableNumber ?? "-"}</span>
            </div>

            {cart.length === 0 && comboCart.length === 0 ? (
              <p className="text-sm text-muted-foreground text-center py-8">No items or combos added</p>
            ) : (
              <div className="space-y-3">
                {cart.map((item) => {
                  const unitPrice = item.price + selectedModifierTotal(item);
                  const missingRequiredModifier = isModifierRequirementMissing(item);
                  return (
                    <div key={item.menuItemId} className="p-3 rounded-lg bg-muted/50 border border-border/50">
                      <div className="flex items-center justify-between">
                        <p className="text-sm font-medium text-foreground truncate">{item.name}</p>
                        <button onClick={() => updateQty(item.menuItemId, -item.quantity)} className="p-1 text-muted-foreground hover:text-destructive">
                          <X className="w-3.5 h-3.5" />
                        </button>
                      </div>
                      {item.allergens.length > 0 && (
                        <p className="text-[10px] text-amber-600 flex items-center gap-0.5 mt-0.5">
                          <AlertTriangle className="w-2.5 h-2.5" />
                          {item.allergens.join(", ")}
                        </p>
                      )}
                      <div className="flex items-center gap-2 mt-2">
                        <button onClick={() => updateQty(item.menuItemId, -1)} className="p-1 rounded bg-background border border-border text-muted-foreground hover:text-foreground"><Minus className="w-3 h-3" /></button>
                        <span className="text-sm font-medium text-foreground w-5 text-center">{item.quantity}</span>
                        <button onClick={() => updateQty(item.menuItemId, 1)} className="p-1 rounded bg-background border border-border text-muted-foreground hover:text-foreground"><Plus className="w-3 h-3" /></button>
                        <span className="text-sm text-foreground ml-auto font-medium">${(unitPrice * item.quantity).toFixed(2)}</span>
                      </div>
                      {item.modifierGroups.length > 0 && (
                        <div className="mt-3 space-y-2 rounded-md border border-border/60 bg-background/60 p-2">
                          {item.modifierGroups.map((group) => (
                            <div key={group.id}>
                              <div className="flex items-center justify-between gap-2">
                                <p className="text-[11px] font-medium text-foreground">{group.name}</p>
                                <span className="text-[10px] text-muted-foreground">
                                  {group.required ? "Required" : "Optional"} · {group.minSelect}-{group.maxSelect}
                                </span>
                              </div>
                              <div className="mt-1 flex flex-wrap gap-2">
                                {group.options.map((option) => (
                                  <label key={option.id} className={`inline-flex items-center gap-1 rounded-md border px-2 py-1 text-[11px] ${option.active ? "border-border bg-card text-foreground" : "border-border/50 bg-muted text-muted-foreground"}`}>
                                    <input
                                      type="checkbox"
                                      checked={item.modifierOptionIds.includes(option.id)}
                                      disabled={!option.active}
                                      onChange={() => toggleModifier(item.menuItemId, group.id, option.id)}
                                      className="h-3 w-3"
                                    />
                                    <span>{option.name}</span>
                                    {Number(option.extraPrice) > 0 && <span className="text-muted-foreground">+${Number(option.extraPrice).toFixed(2)}</span>}
                                  </label>
                                ))}
                              </div>
                            </div>
                          ))}
                          {missingRequiredModifier && <p className="text-[10px] text-destructive">Complete required modifier selections before sending.</p>}
                        </div>
                      )}
                      <Input
                        value={item.note}
                        onChange={(event) => updateNote(item.menuItemId, event.target.value)}
                        placeholder="Add note (allergy, modifications...)"
                        className="mt-2 h-7 text-xs"
                      />
                      <label className="mt-2 flex items-center gap-2 text-[11px] text-muted-foreground">
                        <input type="checkbox" checked={item.sendLater} onChange={() => toggleSendLater(item.menuItemId)} className="h-3 w-3" />
                        Hold for later service instead of sending immediately
                      </label>
                    </div>
                  );
                })}

                {comboCart.map((combo) => {
                  const comboTotal = (combo.price + selectedComboExtraTotal(combo)) * combo.quantity;
                  const missingSelections = isComboRequirementMissing(combo);
                  return (
                    <div key={combo.comboId} className="p-3 rounded-lg bg-muted/50 border border-border/50">
                      <div className="flex items-center justify-between">
                        <div>
                          <p className="text-sm font-medium text-foreground">{combo.name}</p>
                          <p className="text-[11px] text-muted-foreground">{combo.description}</p>
                        </div>
                        <button onClick={() => updateComboQty(combo.comboId, -combo.quantity)} className="p-1 text-muted-foreground hover:text-destructive">
                          <X className="w-3.5 h-3.5" />
                        </button>
                      </div>
                      <div className="flex items-center gap-2 mt-2">
                        <button onClick={() => updateComboQty(combo.comboId, -1)} className="p-1 rounded bg-background border border-border text-muted-foreground hover:text-foreground"><Minus className="w-3 h-3" /></button>
                        <span className="text-sm font-medium text-foreground w-5 text-center">{combo.quantity}</span>
                        <button onClick={() => updateComboQty(combo.comboId, 1)} className="p-1 rounded bg-background border border-border text-muted-foreground hover:text-foreground"><Plus className="w-3 h-3" /></button>
                        <span className="text-sm text-foreground ml-auto font-medium">${comboTotal.toFixed(2)}</span>
                      </div>
                      <div className="mt-3 space-y-2 rounded-md border border-border/60 bg-background/60 p-2">
                        {combo.groups.map((group) => {
                          const selectedIds = combo.selectedOptionsByGroup[group.id] ?? [];
                          return (
                            <div key={group.id}>
                              <div className="flex items-center justify-between gap-2">
                                <p className="text-[11px] font-medium text-foreground">{group.name}</p>
                                <span className="text-[10px] text-muted-foreground">
                                  {group.required ? "Required" : "Optional"} · {group.minSelections}-{group.maxSelections}
                                </span>
                              </div>
                              <div className="mt-1 flex flex-wrap gap-2">
                                {group.options.map((option) => (
                                  <label key={option.id} className={`inline-flex items-center gap-1 rounded-md border px-2 py-1 text-[11px] ${option.active ? "border-border bg-card text-foreground" : "border-border/50 bg-muted text-muted-foreground"}`}>
                                    <input
                                      type="checkbox"
                                      checked={selectedIds.includes(option.id)}
                                      disabled={!option.active}
                                      onChange={() => toggleComboOption(combo.comboId, group.id, option.id)}
                                      className="h-3 w-3"
                                    />
                                    <span>{option.menuItemName}</span>
                                    {option.extraPrice > 0 && <span className="text-muted-foreground">+${option.extraPrice.toFixed(2)}</span>}
                                  </label>
                                ))}
                              </div>
                            </div>
                          );
                        })}
                        {missingSelections && <p className="text-[10px] text-destructive">Complete required combo selections before sending.</p>}
                      </div>
                      <Input
                        value={combo.allergyNotes}
                        onChange={(event) => updateComboField(combo.comboId, "allergyNotes", event.target.value)}
                        placeholder="Combo allergy notes"
                        className="mt-2 h-7 text-xs"
                      />
                      <Input
                        value={combo.specialInstructions}
                        onChange={(event) => updateComboField(combo.comboId, "specialInstructions", event.target.value)}
                        placeholder="Combo special instructions"
                        className="mt-2 h-7 text-xs"
                      />
                    </div>
                  );
                })}
              </div>
            )}

            {(cart.length > 0 || comboCart.length > 0) && (
              <div className="mt-4 pt-4 border-t border-border">
                <div className="flex justify-between text-sm font-heading font-bold text-foreground">
                  <span>Total</span>
                  <span>${total.toFixed(2)}</span>
                </div>
                <p className="text-[10px] text-muted-foreground mt-1">Prices are saved when the order is confirmed.</p>
                <div className="mt-3 flex gap-2">
                  <Button
                    variant="outline"
                    className="flex-1"
                    onClick={() => void createOrderMutation.mutate(true)}
                    disabled={!selectedSession || hasMissingSelections}
                  >
                    Save Draft
                  </Button>
                  <Button
                    className="flex-1"
                    onClick={() => void createOrderMutation.mutate(false)}
                    disabled={!selectedSession || hasMissingSelections}
                  >
                    <Send className="w-4 h-4 mr-2" />
                    Confirm & Send
                  </Button>
                </div>
              </div>
            )}
          </div>

          <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
            <h2 className="text-sm font-semibold text-foreground mb-3 flex items-center gap-2">
              <ClipboardList className="w-4 h-4 text-primary" />
              Ordered Items
            </h2>
            {isLoading ? (
              <p className="text-sm text-muted-foreground text-center py-4">Loading order items...</p>
            ) : orderedItems.length === 0 ? (
              <p className="text-sm text-muted-foreground text-center py-4">No items ordered yet</p>
            ) : (
              <div className="space-y-2">
                {orderedItems.map((item, index) => {
                  const isFirstOrderItem = orderedItems.findIndex((candidate) => candidate.orderId === item.orderId) === index;
                  return (
                    <div key={item.id} className="p-3 rounded-lg border border-border/50 bg-muted/30">
                      <div className="flex items-center justify-between">
                        <div>
                          <span className="text-sm font-medium text-foreground">{item.quantity}x {item.name}</span>
                          {item.note && <p className="text-xs text-muted-foreground mt-0.5">{item.note}</p>}
                          {isFirstOrderItem && <p className="text-[10px] text-muted-foreground mt-1 font-mono">Order {item.orderId}</p>}
                        </div>
                        <StatusBadge status={item.status} variant={orderStatusVariant(item.status)} />
                      </div>
                      {(item.status !== "served" && item.status !== "cancelled") && (
                        <div className="flex gap-1 mt-2 flex-wrap">
                          {isFirstOrderItem && item.orderStatus === "draft" && (
                            <>
                              <Button
                                size="sm"
                                className="h-6 text-xs"
                                onClick={() => void actionMutation.mutate({
                                  path: ENDPOINTS.orders.confirm(item.orderId),
                                  method: "POST",
                                  successMessage: "Draft order confirmed",
                                })}
                              >
                                <Check className="w-3 h-3 mr-1" /> Confirm Draft
                              </Button>
                              <Button size="sm" variant="ghost" className="h-6 text-xs text-destructive" onClick={() => setShowCancelOrderDialog(item.orderId)}>
                                <Ban className="w-3 h-3 mr-1" /> Cancel Order
                              </Button>
                            </>
                          )}
                          {isFirstOrderItem && item.orderStatus !== "draft" && item.orderStatus !== "cancelled" && (
                            <Button size="sm" variant="ghost" className="h-6 text-xs text-destructive" onClick={() => setShowCancelOrderDialog(item.orderId)}>
                              <Ban className="w-3 h-3 mr-1" /> Cancel Order
                            </Button>
                          )}
                          {item.status === "ready" && (
                            <Button
                              size="sm"
                              className="h-6 text-xs"
                              onClick={() => void actionMutation.mutate({
                                path: ENDPOINTS.orders.serveItem(item.id),
                                successMessage: "Item marked as served",
                              })}
                            >
                              <Check className="w-3 h-3 mr-1" /> Served
                            </Button>
                          )}
                          {item.status === "delayed" && (
                            <Button
                              size="sm"
                              variant="outline"
                              className="h-6 text-xs"
                              onClick={() => void actionMutation.mutate({
                                path: ENDPOINTS.orders.sendItem(item.id),
                                method: "POST",
                                successMessage: "Delayed item sent to kitchen",
                              })}
                            >
                              <Send className="w-3 h-3 mr-1" /> Send
                            </Button>
                          )}
                          {item.orderStatus !== "draft" && item.status !== "delayed" && item.status !== "cancelled" && (
                            <Button
                              size="sm"
                              variant="outline"
                              className="h-6 text-xs"
                              onClick={() => void actionMutation.mutate({
                                path: ENDPOINTS.orders.delayItem(item.id),
                                successMessage: "Item marked as delayed",
                              })}
                            >
                              <Clock className="w-3 h-3 mr-1" /> Delay
                            </Button>
                          )}
                          <Button size="sm" variant="ghost" className="h-6 text-xs text-destructive" onClick={() => setShowCancelItemDialog(item.id)}>
                            <Ban className="w-3 h-3 mr-1" /> Cancel Item
                          </Button>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      <ConfirmDialog
        open={!!showCancelOrderDialog}
        onOpenChange={() => setShowCancelOrderDialog(null)}
        title="Cancel Order"
        description="This will cancel every active line in the selected order."
        confirmLabel="Cancel Order"
        variant="destructive"
        requireReason
        reasonLabel="Cancellation Reason"
        onConfirm={(reason) => {
          if (showCancelOrderDialog) {
            void actionMutation.mutate({
              path: ENDPOINTS.orders.cancel(showCancelOrderDialog),
              method: "POST",
              body: { reason: reason ?? "" },
              successMessage: "Order cancelled",
            });
          }
          setShowCancelOrderDialog(null);
        }}
      />

      <ConfirmDialog
        open={!!showCancelItemDialog}
        onOpenChange={() => setShowCancelItemDialog(null)}
        title="Cancel Order Item"
        description="This item has been sent to the kitchen. Please provide a reason for cancellation."
        confirmLabel="Cancel Item"
        variant="destructive"
        requireReason
        reasonLabel="Cancellation Reason"
        onConfirm={(reason) => {
          if (showCancelItemDialog) {
            void actionMutation.mutate({
              path: ENDPOINTS.orders.cancelItem(showCancelItemDialog),
              method: "POST",
              body: { reason: reason ?? "" },
              successMessage: "Item cancelled",
            });
          }
          setShowCancelItemDialog(null);
        }}
      />
    </div>
  );
}

function normalizeOrderStatus(status: string, orderStatus: string) {
  if (orderStatus === "draft") {
    if (status === "hold_for_service") {
      return "draft delayed";
    }
    return "draft";
  }
  switch (status) {
    case "sent_to_kitchen":
      return "pending";
    case "hold_for_service":
      return "delayed";
    case "blocked":
      return "cancelled";
    default:
      return status;
  }
}
