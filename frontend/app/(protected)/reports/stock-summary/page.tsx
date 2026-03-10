 "use client"
 
import { useCallback, useEffect, useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import { getStockSummary, StockSummary } from "@/lib/reports"
import { StockAdjustmentDialog } from "@/components/reports/StockAdjustmentDialog"
import { SmartSelect } from "@/components/ui-kit/SmartSelect"
import { Party, listParties } from "@/lib/parties"

export default function StockSummaryPage() {
  const [rows, setRows] = useState<StockSummary[]>([])
  const [vendors, setVendors] = useState<Party[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState("")
  const [vendorId, setVendorId] = useState("")

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await getStockSummary()
      setRows(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load report")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    let active = true
    const loadVendors = async () => {
      try {
        const data = await listParties("VENDOR")
        if (active) setVendors(data)
      } catch {
        if (active) setVendors([])
      }
    }
    loadVendors()
    return () => {
      active = false
    }
  }, [])

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return rows.filter((r) => {
      const matchesVendor = !vendorId || r.vendorId === vendorId
      const matchesText =
        !q ||
        r.itemName.toLowerCase().includes(q) ||
        r.itemCode.toLowerCase().includes(q) ||
        r.warehouseName.toLowerCase().includes(q) ||
        (r.vendorName ?? "").toLowerCase().includes(q)
      return matchesVendor && matchesText
    })
  }, [query, rows, vendorId])

  return (
    <div className="space-y-6">
      <PageHeader
        title="Stock Summary"
        description="Current stock by item and warehouse."
      />

      {error ? <div className="text-sm text-destructive">{error}</div> : null}

      <DataTablePro
        columns={[
          { key: "itemName", header: "Item", sortable: true },
          { key: "itemCode", header: "Code", sortable: true },
          { key: "vendorName", header: "Vendor", sortable: true, cell: (row) => row.vendorName || "-" },
          { key: "warehouseName", header: "Warehouse", sortable: true },
          { key: "currentStock", header: "Stock", sortable: true },
          { key: "uom", header: "UOM", sortable: true },
          {
            key: "actions",
            header: "",
            cell: (row) => (
              <StockAdjustmentDialog
                itemId={row.itemId}
                itemName={row.itemName}
                warehouseId={row.warehouseId}
                warehouseName={row.warehouseName}
                onSuccess={load}
              />
            ),
          },
        ]}
        data={filtered}
        loading={loading}
        filters={
          <div className="flex flex-wrap items-center gap-2">
            <input
              className="w-full max-w-xs px-3 py-2 rounded-md border border-input bg-background"
              placeholder="Search item/code/warehouse"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
            <div className="w-full max-w-[260px]">
              <SmartSelect<Party>
                value={vendorId}
                onSelect={(next) => setVendorId(next === "__all__" ? "" : next)}
                placeholder="All Vendors"
                searchPlaceholder="Search vendor..."
                options={[{ id: "__all__", name: "All Vendors", type: "VENDOR" } as Party, ...vendors]}
                labelKey="name"
                valueKey="id"
                filterOption={(p, q) => p.name.toLowerCase().includes(q)}
              />
            </div>
            <Button variant="outline" onClick={load} disabled={loading}>
              Refresh
            </Button>
          </div>
        }
        actions={null}
        empty={{
          title: "No stock data",
          description: "Try adjusting filters",
        }}
      />
    </div>
  )
}
 
