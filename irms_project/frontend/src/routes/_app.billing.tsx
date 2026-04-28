import { createFileRoute, useNavigate } from "@tanstack/react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { StatusBadge } from "@/components/shared/StatusBadge";
import { SectionHeader } from "@/components/shared/SectionHeader";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { FormDialog } from "@/components/shared/FormDialog";
import { EmptyState } from "@/components/shared/EmptyState";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Receipt, CreditCard, DollarSign, Percent, RefreshCw,
  FileText, Printer, Edit, Wallet,
} from "lucide-react";
import { toast } from "sonner";
import { apiFetch, getStoredToken } from "@/lib/api/client";
import { buildApiUrl, ENDPOINTS } from "@/lib/api/endpoints";
import { mapBillingData } from "@/lib/api/mappers";
import { useAuth } from "@/lib/auth";
import type { BillingOverviewResponse } from "@/lib/api/types";

export const Route = createFileRoute("/_app/billing")({
  validateSearch: (search: Record<string, unknown>) => ({
    billId: typeof search.billId === "string" ? search.billId : undefined,
    sessionId: typeof search.sessionId === "string" ? search.sessionId : undefined,
  }),
  component: BillingPage,
  head: () => ({ meta: [{ title: "Billing & Settlement - IRMS" }] }),
});

function BillingPage() {
  const navigate = useNavigate();
  const { billId, sessionId } = Route.useSearch();
  const queryClient = useQueryClient();
  const { user } = useAuth();
  const [showPromo, setShowPromo] = useState(false);
  const [showRefund, setShowRefund] = useState(false);
  const [showReceipt, setShowReceipt] = useState<"print" | "email" | null>(null);
  const [showEditBill, setShowEditBill] = useState(false);
  const [showPayment, setShowPayment] = useState(false);
  const [promoCode, setPromoCode] = useState("");
  const [paymentMethod, setPaymentMethod] = useState("credit_card");
  const [cashReceived, setCashReceived] = useState("");
  const [tip, setTip] = useState(0);
  const [discount, setDiscount] = useState(0);
  const [selectedPaymentId, setSelectedPaymentId] = useState<string | null>(null);
  const [receiptRecipientAddress, setReceiptRecipientAddress] = useState("");
  const [refundReview, setRefundReview] = useState<{ id: string; action: "approve" | "reject" } | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ["billing", billId ?? null, sessionId ?? null],
    queryFn: () => {
      const params = new URLSearchParams();
      if (billId) params.set("billId", billId);
      if (sessionId) params.set("sessionId", sessionId);
      const path = params.size > 0 ? `${ENDPOINTS.billing.overview}?${params.toString()}` : ENDPOINTS.billing.overview;
      return apiFetch<BillingOverviewResponse>(path);
    },
  });

  const billing = data ? mapBillingData(data) : null;
  const currentBill = useMemo(() => {
    if (!billing?.currentBill) return null;
    return {
      ...billing.currentBill,
      subtotal: Number(billing.currentBill.subtotal),
      taxAmount: Number(billing.currentBill.taxAmount),
      taxRate: Number(billing.currentBill.taxRate),
      serviceFee: Number(billing.currentBill.serviceFee),
      discount: Number(billing.currentBill.discount),
      tipAmount: Number(billing.currentBill.tipAmount),
      total: Number(billing.currentBill.total),
      items: billing.currentBill.items.map((item) => ({
        ...item,
        total: Number(item.total),
      })),
      splits: billing.currentBill.splits.map((split) => ({
        ...split,
        amount: Number(split.amount),
        tipAmount: Number(split.tipAmount),
      })),
      payments: billing.currentBill.payments.map((payment) => ({
        ...payment,
        amount: Number(payment.amount),
      })),
    };
  }, [billing]);
  const billableSessions = billing?.billableSessions ?? [];
  const recentBills = (billing?.recentBills ?? []).map((bill) => ({
    ...bill,
    total: Number(bill.total),
  }));
  const refundQueue = (billing?.refundQueue ?? []).map((refund) => ({
    ...refund,
    amount: Number(refund.amount),
  }));

  useEffect(() => {
    if (currentBill) {
      setTip(currentBill.tipAmount);
      setDiscount(currentBill.discount);
      setSelectedPaymentId(currentBill.payments[0]?.id ?? null);
    }
  }, [currentBill]);

  const refreshBilling = async (message: string) => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["billing"] }),
      queryClient.invalidateQueries({ queryKey: ["dashboard"] }),
      queryClient.invalidateQueries({ queryKey: ["orders"] }),
    ]);
    toast.success(message);
  };

  const createBillMutation = useMutation({
    mutationFn: (tableSessionId: string) =>
      apiFetch(ENDPOINTS.billing.create, {
        method: "POST",
        body: JSON.stringify({ tableSessionId }),
      }),
    onSuccess: async (_, tableSessionId) => {
      await navigate({ to: "/billing", search: { sessionId: tableSessionId } });
      await refreshBilling("Bill created");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const updateBillMutation = useMutation({
    mutationFn: () =>
      apiFetch(currentBill ? ENDPOINTS.billing.update(currentBill.id) : "", {
        method: "PATCH",
        body: JSON.stringify({
          tipAmount: tip,
          discountAmount: discount,
        }),
      }),
    onSuccess: async () => {
      setShowEditBill(false);
      await refreshBilling("Bill updated");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const applyPromoMutation = useMutation({
    mutationFn: () =>
      apiFetch(currentBill ? ENDPOINTS.billing.promotions(currentBill.id) : "", {
        method: "POST",
        body: JSON.stringify({ code: promoCode.trim() }),
      }),
    onSuccess: async () => {
      setPromoCode("");
      setShowPromo(false);
      await refreshBilling("Promotion applied");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const paymentMutation = useMutation({
    mutationFn: () =>
      apiFetch<{ id: string }>(currentBill ? ENDPOINTS.billing.payments(currentBill.id) : "", {
        method: "POST",
        body: JSON.stringify({
          splitId: null,
          method: paymentMethod,
          amount: null,
          amountReceived: paymentMethod === "cash" && cashReceived !== "" ? Number(cashReceived) : null,
        }),
      }),
    onSuccess: async (payment) => {
      setSelectedPaymentId(payment.id);
      setCashReceived("");
      setShowPayment(false);
      await refreshBilling("Payment processed successfully");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const receiptMutation = useMutation({
    mutationFn: (request: { channel: "print" | "email"; recipientAddress?: string | null }) =>
      apiFetch(selectedPaymentId ? ENDPOINTS.billing.receipt(selectedPaymentId) : "", {
        method: "POST",
        body: JSON.stringify({
          channel: request.channel,
          recipientAddress: request.recipientAddress ?? null,
        }),
      }),
    onSuccess: async (_, variables) => {
      setShowReceipt(null);
      setReceiptRecipientAddress("");
      if (variables.channel === "print" && selectedPaymentId) {
        await downloadReceiptDocument(selectedPaymentId);
      }
      await refreshBilling(variables.channel === "print" ? "Receipt printed" : "Digital receipt queued");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const refundMutation = useMutation({
    mutationFn: (reason: string) =>
      apiFetch<{ status: string }>(selectedPaymentId ? ENDPOINTS.billing.refunds(selectedPaymentId) : "", {
        method: "POST",
        body: JSON.stringify({
          amount: null,
          reason,
        }),
      }),
    onSuccess: async (refund) => {
      setShowRefund(false);
      await refreshBilling(refund.status === "pending_review" ? "Refund request queued for manager review" : "Refund processed");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const refundReviewMutation = useMutation({
    mutationFn: (request: { id: string; action: "approve" | "reject"; reason: string }) =>
      apiFetch(ENDPOINTS.billing.refundApproval(request.id), {
        method: "POST",
        body: JSON.stringify({
          action: request.action,
          reason: request.reason,
        }),
      }),
    onSuccess: async (_, request) => {
      setRefundReview(null);
      await refreshBilling(request.action === "approve" ? "Refund approved" : "Refund rejected");
    },
    onError: (error: Error) => toast.error(error.message),
  });

  const selectedPayment = currentBill?.payments.find((payment) => payment.id === selectedPaymentId) ?? null;
  const payableAmount = currentBill?.total ?? 0;
  const canApproveRefund = Boolean(user?.permissions.includes("all") || user?.roles.some((role) => role === "manager" || role === "admin"));

  const openReceipt = (channel: "print" | "email") => {
    if (!selectedPaymentId) {
      toast.error("Select a payment record first.");
      return;
    }
    if (channel === "print") {
      void receiptMutation.mutate({ channel });
      return;
    }
    setShowReceipt(channel);
  };

  const openRefund = () => {
    if (!selectedPaymentId) {
      toast.error("Select a payment record first.");
      return;
    }
    setShowRefund(true);
  };

  return (
    <div className="space-y-6">
      <SectionHeader
        title="Billing & Settlement"
        description="Review active bills, take payment, issue receipts, and handle refunds."
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 bg-card rounded-xl border border-border p-5 shadow-sm">
          {isLoading ? (
            <div className="py-12 text-center text-sm text-muted-foreground">Loading bill...</div>
          ) : !currentBill ? (
            <EmptyState
              icon={Receipt}
              title="No active bill"
              description="Create a bill from an active table session that already has order items."
              action={billableSessions.length === 0 ? undefined : (
                <div className="space-y-2 min-w-72">
                  {billableSessions.map((session) => (
                    <Button
                      key={session.sessionId}
                      variant="outline"
                      className="w-full justify-between"
                      onClick={() => void createBillMutation.mutate(session.sessionId)}
                    >
                      <span>Table #{session.tableNumber} - {session.guests} guests</span>
                      <span className="text-xs text-muted-foreground">{session.openedAt}</span>
                    </Button>
                  ))}
                </div>
              )}
            />
          ) : (
            <>
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h2 className="text-sm font-semibold text-foreground flex items-center gap-2">
                    <Receipt className="w-4 h-4 text-primary" />
                    {currentBill.id}
                  </h2>
                  <p className="text-xs text-muted-foreground mt-0.5">
                    Table #{currentBill.tableNumber} · Session {currentBill.tableSessionId}
                  </p>
                </div>
                <StatusBadge status={currentBill.status} variant={currentBill.status === "paid" ? "success" : "warning"} />
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-sm mb-4">
                  <thead>
                    <tr className="border-b border-border">
                      <th className="text-left py-2 text-xs font-medium text-muted-foreground uppercase">Item</th>
                      <th className="text-center py-2 text-xs font-medium text-muted-foreground uppercase">Qty</th>
                      <th className="text-right py-2 text-xs font-medium text-muted-foreground uppercase">Price</th>
                      <th className="text-right py-2 text-xs font-medium text-muted-foreground uppercase">Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {currentBill.items.map((item) => (
                      <tr key={item.lineId} className="border-b border-border/30">
                        <td className="py-2.5">
                          <span className="text-sm text-foreground">{item.name}</span>
                          {item.modifiers && <span className="text-xs text-muted-foreground block">{item.modifiers}</span>}
                        </td>
                        <td className="py-2.5 text-sm text-center text-muted-foreground">{item.quantity}</td>
                        <td className="py-2.5 text-sm text-right text-muted-foreground">
                          ${(item.total / Math.max(item.quantity, 1)).toFixed(2)}
                        </td>
                        <td className="py-2.5 text-sm text-right text-foreground font-medium">${item.total.toFixed(2)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="space-y-1.5 text-sm border-t border-border pt-3">
                <div className="flex justify-between text-muted-foreground"><span>Subtotal</span><span>${currentBill.subtotal.toFixed(2)}</span></div>
                <div className="flex justify-between text-muted-foreground"><span>Tax ({currentBill.taxRate.toFixed(1)}%)</span><span>${currentBill.taxAmount.toFixed(2)}</span></div>
                <div className="flex justify-between text-muted-foreground"><span>Service Fee</span><span>${currentBill.serviceFee.toFixed(2)}</span></div>
                {currentBill.discount > 0 && (
                  <div className="flex justify-between text-emerald-600"><span>Discount</span><span>-${currentBill.discount.toFixed(2)}</span></div>
                )}
                <div className="flex justify-between text-muted-foreground"><span>Tip</span><span>${currentBill.tipAmount.toFixed(2)}</span></div>
                <div className="flex justify-between text-foreground font-heading font-bold text-lg pt-2 border-t border-border">
                  <span>Total</span><span>${currentBill.total.toFixed(2)}</span>
                </div>
              </div>

              <div className="flex flex-wrap gap-2 mt-5">
                <Button className="flex-1" onClick={() => setShowPayment(true)}>
                  <CreditCard className="w-4 h-4 mr-2" /> Process Payment
                </Button>
                <Button variant="outline" onClick={() => setShowPromo(true)}>
                  <Percent className="w-4 h-4 mr-2" /> Promo
                </Button>
                <Button variant="outline" onClick={() => setShowEditBill(true)}>
                  <Edit className="w-4 h-4 mr-2" /> Edit Bill
                </Button>
              </div>

              <div className="flex gap-2 mt-3">
                <Button variant="ghost" size="sm" onClick={() => openReceipt("print")}>
                  <Printer className="w-3.5 h-3.5 mr-1" /> Print Receipt
                </Button>
                <Button variant="ghost" size="sm" onClick={() => openReceipt("email")}>
                  <FileText className="w-3.5 h-3.5 mr-1" /> Email Receipt
                </Button>
                <Button variant="ghost" size="sm" className="text-destructive" onClick={openRefund}>
                  <RefreshCw className="w-3.5 h-3.5 mr-1" /> Refund
                </Button>
              </div>

              {currentBill.splits.length > 0 && currentBill.splits.some((split) => split.status === "__hidden__") && (
                <div className="mt-4 rounded-lg border border-border/60 bg-muted/30 p-3">
                  <p className="text-xs font-medium text-foreground">Split Allocation</p>
                  <div className="mt-2 space-y-1">
                    {currentBill.splits.map((split) => (
                      <div key={split.id} className="flex items-center justify-between text-xs text-muted-foreground">
                        <span>{split.label} · {split.status}</span>
                        <span>
                          ${split.amount.toFixed(2)}
                          {split.tipAmount > 0 ? ` · tip $${split.tipAmount.toFixed(2)}` : ""}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {currentBill.payments.length > 0 && (
                <div className="mt-4 rounded-lg border border-border/60 bg-muted/30 p-3">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="text-xs font-medium text-foreground">Payment Record</p>
                      <p className="text-[10px] text-muted-foreground">Select the payment used for receipt delivery or refunds.</p>
                    </div>
                    <select
                      value={selectedPaymentId ?? ""}
                      onChange={(event) => setSelectedPaymentId(event.target.value || null)}
                      className="rounded-md border border-border bg-background px-3 py-2 text-xs"
                    >
                      {currentBill.payments.map((payment) => (
                        <option key={payment.id} value={payment.id}>
                          {payment.method} - ${payment.amount.toFixed(2)} - {payment.paidAt}
                        </option>
                      ))}
                    </select>
                  </div>
                  {selectedPayment && (
                    <p className="mt-2 text-[10px] text-muted-foreground">
                      {selectedPayment.splitLabel ? `Linked split: ${selectedPayment.splitLabel}. ` : "Whole bill payment. "}
                      Status: {selectedPayment.status}
                    </p>
                  )}
                </div>
              )}
            </>
          )}
        </div>

        <div className="space-y-4">
          <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
            <h2 className="text-sm font-semibold text-foreground mb-4">Recent Bills</h2>
            <div className="space-y-3">
              {recentBills.map((bill) => (
                <button
                  key={bill.id}
                  type="button"
                  className="w-full text-left p-3 rounded-lg bg-muted/50 border border-border/50 hover:bg-muted/70 transition-colors"
                  onClick={() => void navigate({ to: "/billing", search: { billId: bill.id } })}
                >
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-xs text-foreground">{bill.id}</span>
                    <StatusBadge status={bill.status} variant={bill.status === "paid" ? "success" : "danger"} />
                  </div>
                  <div className="flex items-center justify-between mt-1.5">
                    <span className="text-xs text-muted-foreground">Table #{bill.tableNumber} · {bill.method}</span>
                    <span className="text-sm font-medium text-foreground">${bill.total.toFixed(2)}</span>
                  </div>
                  <span className="text-[10px] text-muted-foreground">{bill.time}</span>
                </button>
              ))}
            </div>
          </div>

          <div className="bg-card rounded-xl border border-border p-5 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-semibold text-foreground">Refund Review Queue</h2>
              <span className="text-xs text-muted-foreground">{refundQueue.length} pending</span>
            </div>
            {refundQueue.length === 0 ? (
              <p className="text-xs text-muted-foreground">No refunds require manager review.</p>
            ) : (
              <div className="space-y-3">
                {refundQueue.map((refund) => (
                  <div key={refund.id} className="p-3 rounded-lg bg-muted/50 border border-border/50">
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-xs font-medium text-foreground">Table #{refund.tableNumber}</span>
                      <StatusBadge status={refund.status.replace("_", " ")} variant="warning" />
                    </div>
                    <p className="mt-1 text-xs text-muted-foreground">
                      ${refund.amount.toFixed(2)} · {refund.paymentMethod} · {refund.createdAt}
                    </p>
                    <p className="mt-1 text-[10px] text-muted-foreground">
                      Requested by {refund.requestedBy}: {refund.reason}
                    </p>
                    {canApproveRefund && (
                      <div className="mt-2 flex gap-2">
                        <Button size="sm" variant="outline" className="h-7 text-xs" onClick={() => setRefundReview({ id: refund.id, action: "approve" })}>
                          Approve
                        </Button>
                        <Button size="sm" variant="ghost" className="h-7 text-xs text-destructive" onClick={() => setRefundReview({ id: refund.id, action: "reject" })}>
                          Reject
                        </Button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      <FormDialog
        open={showPromo}
        onOpenChange={setShowPromo}
        title="Apply Promotion"
        description="Enter a promo or discount code"
        submitLabel="Apply"
        onSubmit={() => void applyPromoMutation.mutate()}
        submitDisabled={!promoCode.trim()}
      >
        <div className="space-y-2">
          <Label>Promo Code</Label>
          <Input value={promoCode} onChange={(event) => setPromoCode(event.target.value)} placeholder="Enter code (e.g. LOYAL10)" />
        </div>
      </FormDialog>

      <ConfirmDialog
        open={showRefund}
        onOpenChange={setShowRefund}
        title="Issue Refund"
        description="This action is auditable and requires a reason."
        confirmLabel="Process Refund"
        variant="destructive"
        requireReason
        reasonLabel="Refund Reason"
        onConfirm={(reason) => {
          void refundMutation.mutate(reason ?? "");
        }}
      />

      <ConfirmDialog
        open={refundReview !== null}
        onOpenChange={(open) => {
          if (!open) setRefundReview(null);
        }}
        title={refundReview?.action === "approve" ? "Approve Refund" : "Reject Refund"}
        description="Manager review is recorded in the audit log."
        confirmLabel={refundReview?.action === "approve" ? "Approve" : "Reject"}
        variant={refundReview?.action === "reject" ? "destructive" : "default"}
        requireReason={refundReview?.action === "reject"}
        reasonLabel="Review Reason"
        onConfirm={(reason) => {
          if (!refundReview) return;
          void refundReviewMutation.mutate({ ...refundReview, reason: reason ?? "" });
        }}
      />

      <FormDialog
        open={showReceipt !== null}
        onOpenChange={() => setShowReceipt(null)}
        title={showReceipt === "email" ? "Email Receipt" : "Issue Receipt"}
        description={selectedPayment ? `Payment ${selectedPayment.id}` : "Select a payment record before issuing a receipt."}
        submitLabel={showReceipt === "email" ? "Queue Delivery" : "Issue Receipt"}
        onSubmit={() => void receiptMutation.mutate({ channel: showReceipt ?? "print", recipientAddress: receiptRecipientAddress || null })}
        submitDisabled={!selectedPaymentId || (showReceipt === "email" && receiptRecipientAddress.trim() === "")}
      >
        <div className="space-y-3">
          <div className="space-y-2">
            <Label>Payment Record</Label>
            <select
              value={selectedPaymentId ?? ""}
              onChange={(event) => setSelectedPaymentId(event.target.value || null)}
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm"
            >
              <option value="">Select payment</option>
              {currentBill?.payments.map((payment) => (
                <option key={payment.id} value={payment.id}>
                  {payment.method} - ${payment.amount.toFixed(2)} - {payment.paidAt}
                </option>
              ))}
            </select>
          </div>
          {showReceipt === "email" && (
            <div className="space-y-2">
              <Label>Recipient Address</Label>
              <Input
                type="email"
                value={receiptRecipientAddress}
                onChange={(event) => setReceiptRecipientAddress(event.target.value)}
                placeholder="guest@example.com"
              />
            </div>
          )}
        </div>
      </FormDialog>

      <FormDialog
        open={showEditBill}
        onOpenChange={setShowEditBill}
        title="Edit Bill"
        description="Make manual corrections to the bill"
        onSubmit={() => void updateBillMutation.mutate()}
      >
        <div className="space-y-3">
          <div className="space-y-2">
            <Label>Tip Amount ($)</Label>
            <Input type="number" value={tip} onChange={(event) => setTip(Number(event.target.value) || 0)} step="0.01" />
          </div>
          <div className="space-y-2">
            <Label>Manual Discount ($)</Label>
            <Input type="number" value={discount} onChange={(event) => setDiscount(Number(event.target.value) || 0)} step="0.01" />
          </div>
        </div>
      </FormDialog>

      <FormDialog
        open={showPayment}
        onOpenChange={setShowPayment}
        title="Process Payment"
        description={`Amount due: $${payableAmount.toFixed(2)}`}
        submitLabel="Confirm Payment"
        onSubmit={() => void paymentMutation.mutate()}
        submitDisabled={!currentBill || (paymentMethod === "cash" && (cashReceived === "" || Number(cashReceived) < payableAmount))}
      >
        <div className="space-y-4">
          <div className="space-y-1 rounded-lg border border-border/60 bg-muted/30 p-3">
            <p className="text-xs text-muted-foreground">This payment will be applied to the selected bill.</p>
            <p className="text-sm font-medium text-foreground">${payableAmount.toFixed(2)}</p>
          </div>
          <Label>Payment Method</Label>
          <div className="grid grid-cols-2 gap-2">
            {[
              { key: "cash", label: "Cash", icon: DollarSign },
              { key: "credit_card", label: "Credit Card", icon: CreditCard },
              { key: "mobile_payment", label: "Mobile Pay", icon: Wallet },
              { key: "gift_card", label: "Gift Card", icon: FileText },
            ].map((method) => (
              <button
                key={method.key}
                onClick={() => setPaymentMethod(method.key)}
                className={`p-3 rounded-lg border-2 flex items-center gap-2 text-sm transition-colors ${
                  paymentMethod === method.key ? "border-primary bg-primary/5 text-primary" : "border-border text-muted-foreground hover:border-primary/30"
                }`}
              >
                <method.icon className="w-4 h-4" />
                {method.label}
              </button>
            ))}
          </div>
          {paymentMethod === "cash" && (
            <div className="space-y-2">
              <Label>Cash Received</Label>
              <Input
                type="number"
                step="0.01"
                min={payableAmount.toFixed(2)}
                value={cashReceived}
                onChange={(event) => setCashReceived(event.target.value)}
                placeholder={payableAmount.toFixed(2)}
              />
            </div>
          )}
        </div>
      </FormDialog>
    </div>
  );
}

async function downloadReceiptDocument(paymentId: string) {
  const token = getStoredToken();
  const response = await fetch(buildApiUrl(ENDPOINTS.billing.receiptDocument(paymentId)), {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  if (!response.ok) {
    throw new Error("Receipt download failed.");
  }
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `receipt-${paymentId}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
