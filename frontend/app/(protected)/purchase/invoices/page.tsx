"use client"

import Link from "next/link"
import { useEffect, useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import { listPurchaseInvoices, PurchaseInvoice } from "@/lib/purchase-invoices"
import { DataTablePro } from "@/components/ui-kit/DataTablePro"

export default function PurchaseInvoicesListPage() {
  const [rows, setRows] = useState<PurchaseInvoice[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState("")

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const data = await listPurchaseInvoices()
      setRows(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load purchase invoices")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
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
          { key: "invoiceDate", header: "Date" },
          { key: "invoiceNumber", header: "Number", cell: (row) => (
            <Link href={`/purchase/invoices/${row.id}`} className="text-primary hover:underline">
              {row.invoiceNumber || "Draft"}
            </Link>
          )},
          { key: "vendorInvoiceNumber", header: "Vendor Ref", cell: (row) => row.vendorInvoiceNumber || "-" },
          { key: "partyName", header: "Vendor", cell: (row) => row.partyName || "N/A" },
          { key: "warehouseName", header: "Warehouse", cell: (row) => row.warehouseName || "N/A" },
          { key: "grandTotal", header: "Total", cell: (row) => row.grandTotal?.toFixed(2) || "0.00" },
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
