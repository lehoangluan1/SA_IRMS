import { createFileRoute } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { SectionHeader } from "@/components/shared/SectionHeader";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { FormDialog } from "@/components/shared/FormDialog";
import { EmptyState } from "@/components/shared/EmptyState";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Plus, Search, Edit, Trash2, ToggleLeft, Tag, AlertTriangle,
} from "lucide-react";
import { toast } from "sonner";
import { apiFetch } from "@/lib/api/client";
import { ENDPOINTS } from "@/lib/api/endpoints";
import { mapMenuData } from "@/lib/api/mappers";
import type { MenuOverviewResponse } from "@/lib/api/types";

export const Route = createFileRoute("/_app/menu")({
  component: MenuPage,
  head: () => ({ meta: [{ title: "Menu Management - IRMS" }] }),
});

type ActiveTab = "items" | "categories" | "promos" | "combos";

interface CategoryData {
  id: string;
  name: string;
  items: number;
  active: boolean;
}

interface MenuItemData {
  id: string;
  name: string;
  description: string;
  category: string;
  price: number;
  station: string;
  available: boolean;
  allergens: string[];
  modifiers: number;
  version: number;
  ingredients: string[];
}

interface PromoData {
  id: string;
  code: string;
  discount: string;
  validUntil: string;
  active: boolean;
}

interface ComboData {
  id: string;
  name: string;
  description: string;
  price: number;
  active: boolean;
  groups: MenuOverviewResponse["combos"][number]["groups"];
}

interface MenuItemFormState {
  name: string;
  description: string;
  category: string;
  price: string;
  station: string;
  allergens: string;
  ingredients: string;
}

function MenuPage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<ActiveTab>("items");
  const [searchQuery, setSearchQuery] = useState("");

  const { data, isLoading } = useQuery({
    queryKey: ["menu"],
    queryFn: () => apiFetch<MenuOverviewResponse>(ENDPOINTS.menu.overview),
  });

  const menu = data ? mapMenuData(data) : null;
  const categories: CategoryData[] = menu?.categories ?? [];
  const items: MenuItemData[] = (menu?.items ?? []).map((item) => ({
    ...item,
    price: Number(item.price),
  }));
  const promos: PromoData[] = (menu?.promotions ?? []).map((promotion) => ({
    ...promotion,
    validUntil: promotion.validUntil ?? "",
  }));
  const combos: ComboData[] = (menu?.combos ?? []).map((combo) => ({
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
  const categoryNames = categories.filter((category) => category.active).map((category) => category.name);

  const filteredItems = useMemo(
    () =>
      items.filter((item) =>
        searchQuery === "" ? true : item.name.toLowerCase().includes(searchQuery.toLowerCase()),
      ),
    [items, searchQuery],
  );

  const [showAddCategory, setShowAddCategory] = useState(false);
  const [showEditCategory, setShowEditCategory] = useState<CategoryData | null>(null);
  const [showDeleteCategory, setShowDeleteCategory] = useState<CategoryData | null>(null);
  const [showAddItem, setShowAddItem] = useState(false);
  const [showEditItem, setShowEditItem] = useState<MenuItemData | null>(null);
  const [showDeleteItem, setShowDeleteItem] = useState<MenuItemData | null>(null);
  const [showAddPromo, setShowAddPromo] = useState(false);
  const [showEditPromo, setShowEditPromo] = useState<PromoData | null>(null);
  const [showDeletePromo, setShowDeletePromo] = useState<PromoData | null>(null);

  const [newCategory, setNewCategory] = useState({ name: "" });
  const [newItem, setNewItem] = useState<MenuItemFormState>({
    name: "",
    description: "",
    category: "",
    price: "",
    station: "prep",
    allergens: "",
    ingredients: "",
  });
  const [newPromo, setNewPromo] = useState({ code: "", discount: "", validUntil: "" });

  const refreshMenu = async (message: string) => {
    await queryClient.invalidateQueries({ queryKey: ["menu"] });
    toast.success(message);
  };

  const categoryCreateMutation = useMutation({
    mutationFn: () =>
      apiFetch(ENDPOINTS.menu.categories, {
        method: "POST",
        body: JSON.stringify({ name: newCategory.name.trim() }),
      }),
    onSuccess: async () => {
      setNewCategory({ name: "" });
      setShowAddCategory(false);
      await refreshMenu("Category added");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const categoryUpdateMutation = useMutation({
    mutationFn: (category: CategoryData) =>
      apiFetch(ENDPOINTS.menu.category(category.id), {
        method: "PUT",
        body: JSON.stringify({ name: category.name.trim() }),
      }),
    onSuccess: async () => {
      setShowEditCategory(null);
      await refreshMenu("Category updated");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const categoryDeleteMutation = useMutation({
    mutationFn: (categoryId: string) =>
      apiFetch(ENDPOINTS.menu.category(categoryId), {
        method: "DELETE",
      }),
    onSuccess: async () => {
      setShowDeleteCategory(null);
      await refreshMenu("Category deleted");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const itemCreateMutation = useMutation({
    mutationFn: () =>
      apiFetch(ENDPOINTS.menu.items, {
        method: "POST",
        body: JSON.stringify(toMenuItemPayload(newItem)),
      }),
    onSuccess: async () => {
      setNewItem({
        name: "",
        description: "",
        category: categoryNames[0] ?? "",
        price: "",
        station: "prep",
        allergens: "",
        ingredients: "",
      });
      setShowAddItem(false);
      await refreshMenu("Item added");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const itemUpdateMutation = useMutation({
    mutationFn: (item: MenuItemData) =>
      apiFetch(ENDPOINTS.menu.item(item.id), {
        method: "PUT",
        body: JSON.stringify(
          toMenuItemPayload({
            name: item.name,
            description: item.description,
            category: item.category,
            price: String(item.price),
            station: item.station,
            allergens: item.allergens.join(", "),
            ingredients: item.ingredients.join(", "),
          }),
        ),
      }),
    onSuccess: async () => {
      setShowEditItem(null);
      await refreshMenu("Item updated");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const itemAvailabilityMutation = useMutation({
    mutationFn: (itemId: string) =>
      apiFetch(ENDPOINTS.menu.itemAvailability(itemId), {
        method: "PATCH",
      }),
    onSuccess: async () => {
      await refreshMenu("Item availability updated");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const itemDeleteMutation = useMutation({
    mutationFn: (itemId: string) =>
      apiFetch(ENDPOINTS.menu.item(itemId), {
        method: "DELETE",
      }),
    onSuccess: async () => {
      setShowDeleteItem(null);
      await refreshMenu("Item deleted");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const promoCreateMutation = useMutation({
    mutationFn: () =>
      apiFetch(ENDPOINTS.menu.promotions, {
        method: "POST",
        body: JSON.stringify({
          code: newPromo.code.trim().toUpperCase(),
          discount: newPromo.discount.trim(),
          validUntil: newPromo.validUntil || null,
        }),
      }),
    onSuccess: async () => {
      setNewPromo({ code: "", discount: "", validUntil: "" });
      setShowAddPromo(false);
      await refreshMenu("Promotion added");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const promoUpdateMutation = useMutation({
    mutationFn: (promo: PromoData) =>
      apiFetch(ENDPOINTS.menu.promotion(promo.id), {
        method: "PUT",
        body: JSON.stringify({
          code: promo.code.trim().toUpperCase(),
          discount: promo.discount.trim(),
          validUntil: promo.validUntil || null,
        }),
      }),
    onSuccess: async () => {
      setShowEditPromo(null);
      await refreshMenu("Promotion updated");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const promoDeleteMutation = useMutation({
    mutationFn: (promotionId: string) =>
      apiFetch(ENDPOINTS.menu.promotion(promotionId), {
        method: "DELETE",
      }),
    onSuccess: async () => {
      setShowDeletePromo(null);
      await refreshMenu("Promotion deleted");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  return (
    <div className="space-y-6">
      <SectionHeader
        title="Menu Management"
        description="Keep dishes, pricing, and station assignments aligned so the floor and kitchen stay in sync."
        actions={(
          <div className="flex gap-2">
            {activeTab !== "combos" && (
              <Button
                variant="outline"
                onClick={() =>
                  activeTab === "promos"
                    ? setShowAddPromo(true)
                    : activeTab === "categories"
                      ? setShowAddCategory(true)
                      : setShowAddItem(true)
                }
              >
                <Plus className="w-4 h-4 mr-2" />
                {activeTab === "promos" ? "Add Promo" : activeTab === "categories" ? "Add Category" : "Add Item"}
              </Button>
            )}
          </div>
        )}
      />

      <div className="flex gap-1 bg-muted p-1 rounded-lg w-fit">
        {([
          { key: "items" as const, label: "Menu Items" },
          { key: "categories" as const, label: "Categories" },
          { key: "promos" as const, label: "Promotions" },
          { key: "combos" as const, label: "Combos" },
        ]).map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
              activeTab === tab.key ? "bg-card text-foreground shadow-sm" : "text-muted-foreground hover:text-foreground"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === "items" && (
        <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold text-foreground">Menu Items</h2>
            <div className="relative">
              <Search className="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-muted-foreground" />
              <Input
                className="pl-8 w-56 h-8 text-sm"
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                placeholder="Search items..."
              />
            </div>
          </div>
          {isLoading ? (
            <div className="py-10 text-center text-sm text-muted-foreground">Loading menu items...</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border">
                    {["Item", "Category", "Price", "Station", "Allergens", "Ingredients", "Status", "Actions"].map((heading) => (
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
                  {filteredItems.map((item) => (
                    <tr key={item.id} className="border-b border-border/30 hover:bg-muted/30">
                      <td className="py-3 px-2">
                        <span className="text-sm text-foreground font-medium">{item.name}</span>
                        <span className="text-xs text-muted-foreground block">{item.description}</span>
                      </td>
                      <td className="py-3 px-2 text-sm text-muted-foreground">{item.category}</td>
                      <td className="py-3 px-2 text-sm text-foreground font-mono">${item.price.toFixed(2)}</td>
                      <td className="py-3 px-2"><StatusBadge status={item.station} variant="info" /></td>
                      <td className="py-3 px-2">
                        {item.allergens.length > 0 ? (
                          <span className="text-xs text-amber-600 flex items-center gap-0.5">
                            <AlertTriangle className="w-3 h-3" /> {item.allergens.join(", ")}
                          </span>
                        ) : (
                          <span className="text-xs text-muted-foreground">-</span>
                        )}
                      </td>
                      <td className="py-3 px-2 text-xs text-muted-foreground">{item.ingredients.join(", ") || "-"}</td>
                      <td className="py-3 px-2">
                        <StatusBadge
                          status={item.available ? "Active" : "Inactive"}
                          variant={item.available ? "success" : "neutral"}
                        />
                      </td>
                      <td className="py-3 px-2">
                        <div className="flex gap-1">
                          <Button size="sm" variant="ghost" className="h-7" onClick={() => setShowEditItem({ ...item })}>
                            <Edit className="w-3.5 h-3.5" />
                          </Button>
                          <Button
                            size="sm"
                            variant="ghost"
                            className="h-7"
                            onClick={() => void itemAvailabilityMutation.mutate(item.id)}
                          >
                            <ToggleLeft className="w-3.5 h-3.5" />
                          </Button>
                          <Button
                            size="sm"
                            variant="ghost"
                            className="h-7 text-destructive"
                            onClick={() => setShowDeleteItem(item)}
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </Button>
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

      {activeTab === "combos" && (
        <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-sm font-semibold text-foreground">Combo Catalog</h2>
              <p className="text-xs text-muted-foreground mt-1">
                Combos are ready for staff to add during order taking.
              </p>
            </div>
            <span className="text-xs text-muted-foreground">{combos.length} combos</span>
          </div>
          {isLoading ? (
            <div className="py-10 text-center text-sm text-muted-foreground">Loading combos...</div>
          ) : combos.length === 0 ? (
            <EmptyState icon={Tag} title="No combos" description="Create a combo to offer bundled menu items." />
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              {combos.map((combo) => (
                <div key={combo.id} className="rounded-xl border border-border/60 bg-muted/20 p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-sm font-semibold text-foreground">{combo.name}</p>
                      <p className="text-xs text-muted-foreground mt-1">{combo.description}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-sm font-heading font-bold text-primary">${combo.price.toFixed(2)}</p>
                      <StatusBadge status={combo.active ? "Active" : "Inactive"} variant={combo.active ? "success" : "neutral"} />
                    </div>
                  </div>
                  <div className="mt-4 space-y-3">
                    {combo.groups.map((group) => (
                      <div key={group.id} className="rounded-lg border border-border/60 bg-card p-3">
                        <div className="flex items-center justify-between gap-2">
                          <p className="text-xs font-medium text-foreground">{group.name}</p>
                          <span className="text-[10px] text-muted-foreground">
                            {group.required ? "Required" : "Optional"} · {group.minSelections}-{group.maxSelections}
                          </span>
                        </div>
                        <div className="mt-2 space-y-1.5">
                          {group.options.map((option) => (
                            <div key={option.id} className="flex items-center justify-between gap-3 text-[11px]">
                              <div>
                                <span className="text-foreground">{option.menuItemName}</span>
                                <span className="ml-2 text-muted-foreground uppercase">{option.station}</span>
                              </div>
                              <div className="flex items-center gap-2">
                                {option.extraPrice > 0 && <span className="text-muted-foreground">+${option.extraPrice.toFixed(2)}</span>}
                                <StatusBadge status={option.active ? "On" : "Off"} variant={option.active ? "success" : "neutral"} />
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {activeTab === "categories" && (
        <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
          <h2 className="text-sm font-semibold text-foreground mb-4">Categories</h2>
          {isLoading ? (
            <div className="py-10 text-center text-sm text-muted-foreground">Loading categories...</div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
              {categories.map((category) => (
                <div
                  key={category.id}
                  className={`p-4 rounded-xl border ${
                    category.active ? "border-border bg-card" : "border-border/50 bg-muted/30 opacity-60"
                  } shadow-sm`}
                >
                  <div className="flex items-center justify-between">
                    <span className="font-medium text-foreground">{category.name}</span>
                    <StatusBadge
                      status={category.active ? "Active" : "Inactive"}
                      variant={category.active ? "success" : "neutral"}
                    />
                  </div>
                  <p className="text-xs text-muted-foreground mt-1">{category.items} items</p>
                  <div className="flex gap-1 mt-3">
                    <Button size="sm" variant="ghost" className="h-7 text-xs" onClick={() => setShowEditCategory({ ...category })}>
                      <Edit className="w-3 h-3 mr-1" /> Edit
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      className="h-7 text-xs text-destructive"
                      onClick={() => setShowDeleteCategory(category)}
                    >
                      <Trash2 className="w-3 h-3 mr-1" /> Delete
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {activeTab === "promos" && (
        <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
          <h2 className="text-sm font-semibold text-foreground mb-4 flex items-center gap-2">
            <Tag className="w-4 h-4 text-primary" /> Promotions
          </h2>
          {isLoading ? (
            <div className="py-10 text-center text-sm text-muted-foreground">Loading promotions...</div>
          ) : promos.length === 0 ? (
            <EmptyState icon={Tag} title="No promotions" description="Create a promo code to offer discounts" />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border">
                    {["Code", "Discount", "Valid Until", "Status", "Actions"].map((heading) => (
                      <th
                        key={heading}
                        className="text-left py-2.5 px-3 text-xs font-medium text-muted-foreground uppercase tracking-wider"
                      >
                        {heading}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {promos.map((promo) => (
                    <tr key={promo.id} className="border-b border-border/30 hover:bg-muted/30">
                      <td className="py-3 px-3 font-mono font-medium text-foreground">{promo.code}</td>
                      <td className="py-3 px-3 text-muted-foreground">{promo.discount}</td>
                      <td className="py-3 px-3 text-muted-foreground">{promo.validUntil || "-"}</td>
                      <td className="py-3 px-3">
                        <StatusBadge status={promo.active ? "Active" : "Inactive"} variant={promo.active ? "success" : "neutral"} />
                      </td>
                      <td className="py-3 px-3">
                        <div className="flex gap-1">
                          <Button size="sm" variant="ghost" className="h-7" onClick={() => setShowEditPromo({ ...promo })}>
                            <Edit className="w-3.5 h-3.5" />
                          </Button>
                          <Button
                            size="sm"
                            variant="ghost"
                            className="h-7 text-destructive"
                            onClick={() => setShowDeletePromo(promo)}
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </Button>
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
        open={showAddCategory}
        onOpenChange={setShowAddCategory}
        title="Add Category"
        onSubmit={() => void categoryCreateMutation.mutate()}
        submitDisabled={!newCategory.name.trim()}
      >
        <div className="space-y-2">
          <Label>Category Name *</Label>
          <Input
            value={newCategory.name}
            onChange={(event) => setNewCategory({ name: event.target.value })}
            placeholder="e.g. Appetizers"
          />
        </div>
      </FormDialog>

      {showEditCategory && (
        <FormDialog
          open={!!showEditCategory}
          onOpenChange={() => setShowEditCategory(null)}
          title="Edit Category"
          onSubmit={() => void categoryUpdateMutation.mutate(showEditCategory)}
        >
          <div className="space-y-2">
            <Label>Category Name</Label>
            <Input
              value={showEditCategory.name}
              onChange={(event) =>
                setShowEditCategory((current) => (current ? { ...current, name: event.target.value } : null))
              }
            />
          </div>
        </FormDialog>
      )}

      <ConfirmDialog
        open={!!showDeleteCategory}
        onOpenChange={() => setShowDeleteCategory(null)}
        title="Delete Category"
        description={`Delete "${showDeleteCategory?.name}"?`}
        confirmLabel="Delete"
        variant="destructive"
        onConfirm={() => {
          if (showDeleteCategory) {
            void categoryDeleteMutation.mutate(showDeleteCategory.id);
          }
        }}
      />

      <FormDialog
        open={showAddItem}
        onOpenChange={setShowAddItem}
        title="Add Menu Item"
        onSubmit={() => void itemCreateMutation.mutate()}
        submitDisabled={!isMenuItemFormValid(newItem)}
      >
        <div className="grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label>Item Name *</Label>
            <Input value={newItem.name} onChange={(event) => setNewItem((current) => ({ ...current, name: event.target.value }))} />
          </div>
          <div className="space-y-2">
            <Label>Price *</Label>
            <Input
              type="number"
              step="0.01"
              value={newItem.price}
              onChange={(event) => setNewItem((current) => ({ ...current, price: event.target.value }))}
            />
          </div>
          <div className="space-y-2">
            <Label>Category</Label>
            <select
              value={newItem.category || categoryNames[0] || ""}
              onChange={(event) => setNewItem((current) => ({ ...current, category: event.target.value }))}
              className="w-full px-3 py-2 rounded-md border border-border bg-background text-sm"
            >
              {categoryNames.map((name) => (
                <option key={name} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-2">
            <Label>Station</Label>
            <select
              value={newItem.station}
              onChange={(event) => setNewItem((current) => ({ ...current, station: event.target.value }))}
              className="w-full px-3 py-2 rounded-md border border-border bg-background text-sm"
            >
              {["grill", "fryer", "dessert", "drinks", "salad", "prep"].map((station) => (
                <option key={station} value={station}>
                  {station}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div className="space-y-2">
          <Label>Description</Label>
          <Textarea
            value={newItem.description}
            onChange={(event) => setNewItem((current) => ({ ...current, description: event.target.value }))}
            rows={2}
          />
        </div>
        <div className="space-y-2">
          <Label>Ingredients (comma-separated)</Label>
          <Input
            value={newItem.ingredients}
            onChange={(event) => setNewItem((current) => ({ ...current, ingredients: event.target.value }))}
            placeholder="Ribeye Steak:1, Russet Potatoes:0.5"
          />
        </div>
        <div className="space-y-2">
          <Label>Allergens (comma-separated)</Label>
          <Input
            value={newItem.allergens}
            onChange={(event) => setNewItem((current) => ({ ...current, allergens: event.target.value }))}
            placeholder="dairy, gluten"
          />
        </div>
      </FormDialog>

      {showEditItem && (
        <FormDialog
          open={!!showEditItem}
          onOpenChange={() => setShowEditItem(null)}
          title="Edit Menu Item"
          onSubmit={() => void itemUpdateMutation.mutate(showEditItem)}
        >
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Item Name</Label>
              <Input
                value={showEditItem.name}
                onChange={(event) =>
                  setShowEditItem((current) => (current ? { ...current, name: event.target.value } : null))
                }
              />
            </div>
            <div className="space-y-2">
              <Label>Price</Label>
              <Input
                type="number"
                step="0.01"
                value={showEditItem.price}
                onChange={(event) =>
                  setShowEditItem((current) =>
                    current ? { ...current, price: Number(event.target.value) || 0 } : null,
                  )
                }
              />
            </div>
            <div className="space-y-2">
              <Label>Category</Label>
              <select
                value={showEditItem.category}
                onChange={(event) =>
                  setShowEditItem((current) => (current ? { ...current, category: event.target.value } : null))
                }
                className="w-full px-3 py-2 rounded-md border border-border bg-background text-sm"
              >
                {categoryNames.map((name) => (
                  <option key={name} value={name}>
                    {name}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-2">
              <Label>Station</Label>
              <select
                value={showEditItem.station}
                onChange={(event) =>
                  setShowEditItem((current) => (current ? { ...current, station: event.target.value } : null))
                }
                className="w-full px-3 py-2 rounded-md border border-border bg-background text-sm"
              >
                {["grill", "fryer", "dessert", "drinks", "salad", "prep"].map((station) => (
                  <option key={station} value={station}>
                    {station}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="space-y-2">
            <Label>Description</Label>
            <Textarea
              value={showEditItem.description}
              onChange={(event) =>
                setShowEditItem((current) => (current ? { ...current, description: event.target.value } : null))
              }
              rows={2}
            />
          </div>
          <div className="space-y-2">
            <Label>Ingredients (comma-separated)</Label>
            <Input
              value={showEditItem.ingredients.join(", ")}
              onChange={(event) =>
                setShowEditItem((current) =>
                  current
                    ? {
                        ...current,
                        ingredients: splitCommaSeparated(event.target.value),
                      }
                    : null,
                )
              }
            />
          </div>
          <div className="space-y-2">
            <Label>Allergens (comma-separated)</Label>
            <Input
              value={showEditItem.allergens.join(", ")}
              onChange={(event) =>
                setShowEditItem((current) =>
                  current
                    ? {
                        ...current,
                        allergens: splitCommaSeparated(event.target.value),
                      }
                    : null,
                )
              }
            />
          </div>
          <p className="text-xs text-muted-foreground">
            Saving will publish version {showEditItem.version + 1}. Already confirmed orders keep their original prices.
          </p>
        </FormDialog>
      )}

      <ConfirmDialog
        open={!!showDeleteItem}
        onOpenChange={() => setShowDeleteItem(null)}
        title="Delete Menu Item"
        description={`Delete "${showDeleteItem?.name}"? This cannot be undone.`}
        confirmLabel="Delete"
        variant="destructive"
        onConfirm={() => {
          if (showDeleteItem) {
            void itemDeleteMutation.mutate(showDeleteItem.id);
          }
        }}
      />

      <FormDialog
        open={showAddPromo}
        onOpenChange={setShowAddPromo}
        title="Add Promotion"
        onSubmit={() => void promoCreateMutation.mutate()}
        submitDisabled={!newPromo.code.trim() || !newPromo.discount.trim()}
      >
        <div className="space-y-4">
          <div className="space-y-2">
            <Label>Promo Code *</Label>
            <Input
              value={newPromo.code}
              onChange={(event) => setNewPromo((current) => ({ ...current, code: event.target.value.toUpperCase() }))}
              placeholder="LOYAL10"
            />
          </div>
          <div className="space-y-2">
            <Label>Discount *</Label>
            <Input
              value={newPromo.discount}
              onChange={(event) => setNewPromo((current) => ({ ...current, discount: event.target.value }))}
              placeholder="10% off"
            />
          </div>
          <div className="space-y-2">
            <Label>Valid Until</Label>
            <Input
              type="date"
              value={newPromo.validUntil}
              onChange={(event) => setNewPromo((current) => ({ ...current, validUntil: event.target.value }))}
            />
          </div>
        </div>
      </FormDialog>

      {showEditPromo && (
        <FormDialog
          open={!!showEditPromo}
          onOpenChange={() => setShowEditPromo(null)}
          title="Edit Promotion"
          onSubmit={() => void promoUpdateMutation.mutate(showEditPromo)}
        >
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Promo Code</Label>
              <Input
                value={showEditPromo.code}
                onChange={(event) =>
                  setShowEditPromo((current) =>
                    current ? { ...current, code: event.target.value.toUpperCase() } : null,
                  )
                }
              />
            </div>
            <div className="space-y-2">
              <Label>Discount</Label>
              <Input
                value={showEditPromo.discount}
                onChange={(event) =>
                  setShowEditPromo((current) => (current ? { ...current, discount: event.target.value } : null))
                }
              />
            </div>
            <div className="space-y-2">
              <Label>Valid Until</Label>
              <Input
                type="date"
                value={showEditPromo.validUntil}
                onChange={(event) =>
                  setShowEditPromo((current) => (current ? { ...current, validUntil: event.target.value } : null))
                }
              />
            </div>
          </div>
        </FormDialog>
      )}

      <ConfirmDialog
        open={!!showDeletePromo}
        onOpenChange={() => setShowDeletePromo(null)}
        title="Delete Promotion"
        description={`Delete promo "${showDeletePromo?.code}"?`}
        confirmLabel="Delete"
        variant="destructive"
        onConfirm={() => {
          if (showDeletePromo) {
            void promoDeleteMutation.mutate(showDeletePromo.id);
          }
        }}
      />
    </div>
  );
}

function splitCommaSeparated(value: string) {
  return value
    .split(",")
    .map((token) => token.trim())
    .filter(Boolean);
}

function isMenuItemFormValid(form: MenuItemFormState) {
  return (
    form.name.trim() !== ""
    && form.description.trim() !== ""
    && form.category.trim() !== ""
    && form.price.trim() !== ""
    && form.ingredients.trim() !== ""
  );
}

function toMenuItemPayload(form: MenuItemFormState) {
  return {
    name: form.name.trim(),
    description: form.description.trim(),
    category: form.category.trim(),
    price: Number(form.price),
    station: form.station,
    allergens: splitCommaSeparated(form.allergens),
    ingredients: splitCommaSeparated(form.ingredients),
    preparationTimeMin: 10,
  };
}
