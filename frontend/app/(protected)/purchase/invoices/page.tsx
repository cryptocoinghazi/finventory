"use client"

import Link from "next/link"
import { useCallback, useEffect, useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import {
  applyPurchaseInvoicePayment,
  cancelPurchaseInvoice,
  InvoicePaymentStatus,
  listPurchaseInvoices,
  PurchaseInvoice,
} from "@/lib/purchase-invoices"
import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { CreditCard, Trash2 } from "lucide-react"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useToast } from "@/components/ui/use-toast"
import { ConfirmDialog } from "@/components/ui-kit/ConfirmDialog"
import { getCurrentUser } from "@/lib/users"

function InvoicePaymentDialog({
  invoice,
  onUpdated,
}: {
  invoice: PurchaseInvoice
  onUpdated: (updated: PurchaseInvoice) => void
}) {
  const { toast } = useToast()
  const [open, setOpen] = useState(false)
  const [paymentStatus, setPaymentStatus] = useState<InvoicePaymentStatus>(
    invoice.paymentStatus || "PENDING"
  )
  const [paymentAmount, setPaymentAmount] = useState("")
  const [saving, setSaving] = useState(false)

  const grandTotal = invoice.grandTotal ?? 0
  const paidAmount =
    invoice.paidAmount ??
    (invoice.balanceAmount != null ? Math.max(0, grandTotal - invoice.balanceAmount) : 0)
  const balanceAmount =
    invoice.balanceAmount != null ? invoice.balanceAmount : Math.max(0, grandTotal - paidAmount)

  useEffect(() => {
    if (!open) return
    setPaymentStatus(invoice.paymentStatus || "PENDING")
    setPaymentAmount("")
  }, [invoice.paymentStatus, open])

  async function save() {
    const amountNum = Number(paymentAmount || 0)
    if (!Number.isFinite(amountNum) || amountNum < 0) {
      toast({ variant: "destructive", title: "Invalid amount", description: "Enter a valid amount" })
      return
    }

    if (paymentStatus === "PENDING") {
      if (amountNum !== 0) {
        toast({
          variant: "destructive",
          title: "Invalid payment",
          description: "Pending status requires 0 amount",
        })
        return
      }
    }

    if (paymentStatus === "PARTIAL") {
      if (amountNum <= 0) {
        toast({
          variant: "destructive",
          title: "Invalid payment",
          description: "Partial status requires a positive amount",
        })
        return
      }
      if (amountNum > balanceAmount) {
        toast({
          variant: "destructive",
          title: "Invalid payment",
          description: "Payment amount exceeds outstanding balance",
        })
        return
      }
    }

    setSaving(true)
    try {
      const updated = await applyPurchaseInvoicePayment(invoice.id, paymentStatus, amountNum)
      onUpdated(updated)
      toast({ title: "Payment updated" })
      setOpen(false)
    } catch (err) {
      toast({
        variant: "destructive",
        title: "Update failed",
        description: err instanceof Error ? err.message : "Request failed",
      })
    } finally {
      setSaving(false)
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="ghost" size="icon" title="Update payment">
          <CreditCard className="h-4 w-4" />
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Update Payment</DialogTitle>
        </DialogHeader>

        <div className="grid gap-4 py-2">
          <div className="grid grid-cols-4 items-center gap-4">
            <Label className="text-right">Invoice</Label>
            <div className="col-span-3 font-medium">{invoice.invoiceNumber || "Draft"}</div>
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label className="text-right">Total</Label>
            <div className="col-span-3 font-medium">{grandTotal.toFixed(2)}</div>
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label className="text-right">Paid</Label>
            <div className="col-span-3 font-medium">{paidAmount.toFixed(2)}</div>
          </div>
          <div className="grid grid-cols-4 items-center gap-4">
            <Label className="text-right">Balance</Label>
            <div className="col-span-3 font-medium">{balanceAmount.toFixed(2)}</div>
          </div>

          <div className="grid grid-cols-4 items-center gap-4">
            <Label className="text-right">Status</Label>
            <div className="col-span-3">
              <Select
                value={paymentStatus}
                onValueChange={(v) => setPaymentStatus(v as InvoicePaymentStatus)}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="PENDING">Pending</SelectItem>
                  <SelectItem value="PARTIAL">Partial</SelectItem>
                  <SelectItem value="PAID">Paid</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor={`amount-${invoice.id}`} className="text-right">
              Amount
            </Label>
            <Input
              id={`amount-${invoice.id}`}
              className="col-span-3"
              type="number"
              inputMode="decimal"
              value={paymentAmount}
              onChange={(e) => setPaymentAmount(e.target.value)}
              placeholder={paymentStatus === "PARTIAL" ? "Enter payment amount" : "0"}
              disabled={paymentStatus !== "PARTIAL"}
            />
          </div>
        </div>

        <Button onClick={save} disabled={saving}>
          {saving ? "Saving..." : "Save"}
        </Button>
      </DialogContent>
    </Dialog>
  )
}

export default function PurchaseInvoicesListPage() {
  const { toast } = useToast()
  const [rows, setRows] = useState<PurchaseInvoice[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState("")
  const [paymentStatus, setPaymentStatus] = useState<InvoicePaymentStatus | "">("")
  const [fromDate, setFromDate] = useState("")
  const [toDate, setToDate] = useState("")
  const [isAdmin, setIsAdmin] = useState<boolean | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await listPurchaseInvoices({ paymentStatus, fromDate, toDate })
      setRows(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load purchase invoices")
    } finally {
      setLoading(false)
    }
  }, [fromDate, paymentStatus, toDate])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    ;(async () => {
      try {
        const user = await getCurrentUser()
        setIsAdmin(user.role === "ADMIN")
      } catch {
        setIsAdmin(null)
      }
    })()
  }, [])

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return rows.filter((inv) => {
      const matchesText =
        !q ||
        (inv.invoiceNumber ?? "").toLowerCase().includes(q) ||
        (inv.vendorInvoiceNumber ?? "").toLowerCase().includes(q) ||
        (inv.partyName ?? "").toLowerCase().includes(q)
      return matchesText
    })
  }, [query, rows])

  return (
    <div className="space-y-6">
      <PageHeader
        title="Purchase Invoices"
        description="Track purchase invoices from vendors."
        actions={
          <Link href="/purchase/invoices/new">
            <Button>Create Invoice</Button>
          </Link>
        }
      />

      {error ? <div className="text-sm text-destructive">{error}</div> : null}

      <DataTablePro
        columns={[
          { key: "invoiceDate", header: "Date", sortable: true, filterable: true },
          {
            key: "invoiceNumber",
            header: "Number",
            sortable: true,
            filterable: true,
            cell: (row) => (
              <Link href={`/purchase/invoices/${row.id}`} className="text-primary hover:underline">
                {row.invoiceNumber || "Draft"}
              </Link>
            ),
          },
          {
            key: "vendorInvoiceNumber",
            header: "Vendor Ref",
            sortable: true,
            filterable: true,
            cell: (row) => row.vendorInvoiceNumber || "-",
          },
          {
            key: "partyName",
            header: "Vendor",
            sortable: true,
            filterable: true,
            cell: (row) => row.partyName || "N/A",
          },
          {
            key: "warehouseName",
            header: "Warehouse",
            sortable: true,
            filterable: true,
            cell: (row) => row.warehouseName || "N/A",
          },
          {
            key: "paymentStatus",
            header: "Status",
            sortable: true,
            filterable: true,
            cell: (row) => (row.deletedAt ? "CANCELLED" : row.paymentStatus || "PENDING"),
          },
          {
            key: "grandTotal",
            header: "Total",
            sortable: true,
            filterable: true,
            cell: (row) => row.grandTotal?.toFixed(2) || "0.00",
          },
          {
            key: "actions",
            header: "",
            cell: (row) => (
              <div className="flex items-center justify-end gap-1">
                {row.deletedAt ? null : (
                  <InvoicePaymentDialog
                    invoice={row}
                    onUpdated={(updated) => {
                      setRows((prev) => prev.map((r) => (r.id === updated.id ? updated : r)))
                    }}
                  />
                )}
                {isAdmin ? (
                  <ConfirmDialog
                    title="Cancel invoice?"
                    description="This will reverse stock and accounting entries."
                    confirmText="Cancel invoice"
                    onConfirm={async () => {
                      try {
                        const updated = await cancelPurchaseInvoice(row.id)
                        setRows((prev) => prev.map((r) => (r.id === updated.id ? updated : r)))
                        toast({ title: "Invoice cancelled" })
                      } catch (err) {
                        toast({
                          variant: "destructive",
                          title: "Cancel failed",
                          description: err instanceof Error ? err.message : "Request failed",
                        })
                      }
                    }}
                  >
                    <Button variant="ghost" size="icon" title="Cancel invoice" disabled={!!row.deletedAt}>
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </ConfirmDialog>
                ) : null}
              </div>
            ),
          },
        ]}
        data={filtered}
        loading={loading}
        filters={
          <div className="flex flex-wrap items-center gap-2">
            <input
              className="w-full max-w-xs px-3 py-2 rounded-md border border-input bg-background"
              placeholder="Search invoices..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
            <div className="w-full max-w-[180px]">
              <Select
                value={paymentStatus || "__ALL__"}
                onValueChange={(v) =>
                  setPaymentStatus(v === "__ALL__" ? "" : (v as InvoicePaymentStatus))
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="All Status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="__ALL__">All</SelectItem>
                  <SelectItem value="PENDING">Pending</SelectItem>
                  <SelectItem value="PARTIAL">Partial</SelectItem>
                  <SelectItem value="PAID">Paid</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <input
              className="w-full max-w-[170px] px-3 py-2 rounded-md border border-input bg-background"
              type="date"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
            />
            <input
              className="w-full max-w-[170px] px-3 py-2 rounded-md border border-input bg-background"
              type="date"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
            />
            <Button variant="outline" onClick={load} disabled={loading}>
              Refresh
            </Button>
          </div>
        }
        actions={null}
        empty={{
          title: "No purchase invoices found",
          description: "Create your first purchase invoice",
          onAdd: () => {
            window.location.href = "/purchase/invoices/new"
          },
        }}
      />
    </div>
  )
}
