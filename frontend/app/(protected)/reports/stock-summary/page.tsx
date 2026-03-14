 "use client"
 
import { useCallback, useEffect, useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import {
  getLowStockReport,
  getStockMovement,
  getStockReport,
  LowStockResponse,
  StockMovementEntry,
  StockReportRow,
} from "@/lib/reports"
import { StockAdjustmentDialog } from "@/components/reports/StockAdjustmentDialog"
import { SmartSelect } from "@/components/ui-kit/SmartSelect"
import { Party, listParties } from "@/lib/parties"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Item, listItems } from "@/lib/items"
import { Warehouse, listWarehouses } from "@/lib/warehouses"
import { DateRangeFilter } from "@/components/reports/DateRangeFilter"

export default function StockSummaryPage() {
  const [tab, setTab] = useState<"summary" | "low" | "movement">("summary")
  const [rows, setRows] = useState<StockReportRow[]>([])
  const [low, setLow] = useState<LowStockResponse | null>(null)
  const [movement, setMovement] = useState<StockMovementEntry[]>([])
  const [vendors, setVendors] = useState<Party[]>([])
  const [items, setItems] = useState<Item[]>([])
  const [warehouses, setWarehouses] = useState<Warehouse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState("")
  const [vendorId, setVendorId] = useState("")
  const [threshold, setThreshold] = useState("5")
  const [movementFromDate, setMovementFromDate] = useState("")
  const [movementToDate, setMovementToDate] = useState("")
  const [movementItemId, setMovementItemId] = useState("")
  const [movementWarehouseId, setMovementWarehouseId] = useState("")

  const loadSummary = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await getStockReport()
      setRows(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load report")
    } finally {
      setLoading(false)
    }
  }, [])

  const loadLow = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const parsed = Number(threshold)
      const data = await getLowStockReport({
        threshold: Number.isFinite(parsed) ? parsed : undefined,
      })
      setLow(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load report")
      setLow(null)
    } finally {
      setLoading(false)
    }
  }, [threshold])

  const loadMovement = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await getStockMovement({
        fromDate: movementFromDate || undefined,
        toDate: movementToDate || undefined,
        itemId: movementItemId || undefined,
        warehouseId: movementWarehouseId || undefined,
      })
      setMovement(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load report")
      setMovement([])
    } finally {
      setLoading(false)
    }
  }, [movementFromDate, movementItemId, movementToDate, movementWarehouseId])

  const handleAdjustmentSuccess = useCallback(() => {
    if (tab === "summary") return loadSummary()
    if (tab === "low") return loadLow()
    if (tab === "movement") return loadMovement()
  }, [loadLow, loadMovement, loadSummary, tab])

  useEffect(() => {
    if (tab === "summary") loadSummary()
    if (tab === "low") loadLow()
    if (tab === "movement") loadMovement()
  }, [loadLow, loadMovement, loadSummary, tab])

  useEffect(() => {
    let active = true
    const loadLookups = async () => {
      try {
        const [data, it, wh] = await Promise.all([listParties("VENDOR"), listItems(), listWarehouses()])
        if (!active) return
        setVendors(data)
        setItems(it)
        setWarehouses(wh)
      } catch {
        if (!active) return
        setVendors([])
        setItems([])
        setWarehouses([])
      }
    }
    loadLookups()
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

  const lowRows = useMemo(() => {
    const q = query.trim().toLowerCase()
    const base = [
      ...(low?.outOfStock ?? []).map((r) => ({ ...r, bucket: "Out of Stock" })),
      ...(low?.lowStock ?? []).map((r) => ({ ...r, bucket: "Low Stock" })),
    ]
    return base.filter((r) => {
      const matchesVendor = !vendorId || r.vendorId === vendorId
      const matchesText =
        !q ||
        r.itemName.toLowerCase().includes(q) ||
        r.itemCode.toLowerCase().includes(q) ||
        r.warehouseName.toLowerCase().includes(q) ||
        (r.vendorName ?? "").toLowerCase().includes(q)
      return matchesVendor && matchesText
    })
  }, [low, query, vendorId])

  const itemOptions = useMemo(() => [{ id: "__all__", name: "All Items", code: "" } as Item, ...items], [items])
  const warehouseOptions = useMemo(
    () => [{ id: "__all__", name: "All Warehouses" } as Warehouse, ...warehouses],
    [warehouses]
  )

  return (
    <div className="space-y-6">
      <PageHeader
        title="Stock Report"
        description="Current stock, low stock, and movement by item and warehouse."
        actions={
          <StockAdjustmentDialog
            items={items}
            warehouses={warehouses}
            onSuccess={handleAdjustmentSuccess}
            trigger={<Button disabled={loading || items.length === 0 || warehouses.length === 0}>Adjust Stock</Button>}
          />
        }
      />

      {error ? <div className="text-sm text-destructive">{error}</div> : null}

      <Tabs value={tab} onValueChange={(v) => setTab(v as "summary" | "low" | "movement")} className="space-y-4">
        <TabsList>
          <TabsTrigger value="summary">Summary</TabsTrigger>
          <TabsTrigger value="low">Low Stock</TabsTrigger>
          <TabsTrigger value="movement">Movement</TabsTrigger>
        </TabsList>

        <TabsContent value="summary">
          <DataTablePro
            columns={[
              { key: "itemName", header: "Item", sortable: true },
              { key: "itemCode", header: "Code", sortable: true },
              { key: "vendorName", header: "Vendor", sortable: true, cell: (row) => row.vendorName || "-" },
              { key: "warehouseName", header: "Warehouse", sortable: true },
              { key: "currentStock", header: "Stock", sortable: true },
              { key: "uom", header: "UOM", sortable: true },
              { key: "unitPrice", header: "Unit Price", sortable: true },
              { key: "valuation", header: "Valuation", sortable: true },
              {
                key: "actions",
                header: "",
                cell: (row) => (
                  <StockAdjustmentDialog
                    itemId={row.itemId}
                    itemName={row.itemName}
                    warehouseId={row.warehouseId}
                    warehouseName={row.warehouseName}
                    onSuccess={loadSummary}
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
                <Button variant="outline" onClick={loadSummary} disabled={loading}>
                  Refresh
                </Button>
                <StockAdjustmentDialog
                  items={items}
                  warehouses={warehouses}
                  onSuccess={loadSummary}
                  trigger={
                    <Button disabled={loading || items.length === 0 || warehouses.length === 0}>
                      Adjust Stock
                    </Button>
                  }
                />
              </div>
            }
            actions={null}
            empty={{
              title: "No stock data",
              description: "Try adjusting filters",
            }}
          />
        </TabsContent>

        <TabsContent value="low">
          <DataTablePro
            columns={[
              { key: "bucket", header: "Bucket", sortable: true },
              { key: "itemName", header: "Item", sortable: true },
              { key: "itemCode", header: "Code", sortable: true },
              { key: "vendorName", header: "Vendor", sortable: true, cell: (row) => row.vendorName || "-" },
              { key: "warehouseName", header: "Warehouse", sortable: true },
              { key: "currentStock", header: "Stock", sortable: true },
              { key: "uom", header: "UOM", sortable: true },
              { key: "unitPrice", header: "Unit Price", sortable: true },
              { key: "valuation", header: "Valuation", sortable: true },
              {
                key: "actions",
                header: "",
                cell: (row) => (
                  <StockAdjustmentDialog
                    itemId={row.itemId}
                    itemName={row.itemName}
                    warehouseId={row.warehouseId}
                    warehouseName={row.warehouseName}
                    onSuccess={loadLow}
                  />
                ),
              },
            ]}
            data={lowRows as Array<StockReportRow & { bucket: string }>}
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
                <input
                  className="w-full max-w-[140px] px-3 py-2 rounded-md border border-input bg-background"
                  placeholder="Threshold"
                  inputMode="numeric"
                  value={threshold}
                  onChange={(e) => setThreshold(e.target.value)}
                />
                <Button variant="outline" onClick={loadLow} disabled={loading}>
                  Refresh
                </Button>
                <StockAdjustmentDialog
                  items={items}
                  warehouses={warehouses}
                  onSuccess={loadLow}
                  trigger={
                    <Button disabled={loading || items.length === 0 || warehouses.length === 0}>
                      Adjust Stock
                    </Button>
                  }
                />
              </div>
            }
            actions={null}
            empty={{
              title: "No low stock items",
              description: "Try adjusting threshold",
            }}
          />
        </TabsContent>

        <TabsContent value="movement">
          <DataTablePro
            columns={[
              { key: "date", header: "Date", sortable: true },
              { key: "itemName", header: "Item", sortable: true },
              { key: "warehouseName", header: "Warehouse", sortable: true },
              { key: "qtyIn", header: "Qty In", sortable: true },
              { key: "qtyOut", header: "Qty Out", sortable: true },
              { key: "refType", header: "Ref Type", sortable: true, cell: (r) => r.refType || "-" },
              { key: "refId", header: "Ref ID", sortable: true },
            ]}
            data={movement}
            loading={loading}
            filters={
              <div className="flex flex-wrap items-center gap-2">
                <DateRangeFilter
                  fromDate={movementFromDate}
                  toDate={movementToDate}
                  onChange={({ fromDate: f, toDate: t }) => {
                    setMovementFromDate(f)
                    setMovementToDate(t)
                  }}
                />
                <div className="w-full max-w-[260px]">
                  <SmartSelect<Item>
                    value={movementItemId}
                    onSelect={(next) => setMovementItemId(next === "__all__" ? "" : next)}
                    placeholder="All Items"
                    searchPlaceholder="Search item..."
                    options={itemOptions}
                    labelKey="name"
                    valueKey="id"
                    filterOption={(it, q) => (it.name + " " + it.code).toLowerCase().includes(q)}
                  />
                </div>
                <div className="w-full max-w-[260px]">
                  <SmartSelect<Warehouse>
                    value={movementWarehouseId}
                    onSelect={(next) => setMovementWarehouseId(next === "__all__" ? "" : next)}
                    placeholder="All Warehouses"
                    searchPlaceholder="Search warehouse..."
                    options={warehouseOptions}
                    labelKey="name"
                    valueKey="id"
                    filterOption={(w, q) => w.name.toLowerCase().includes(q)}
                  />
                </div>
                <Button variant="outline" onClick={loadMovement} disabled={loading}>
                  Refresh
                </Button>
              </div>
            }
            actions={null}
            empty={{
              title: "No movement",
              description: "Try adjusting filters",
            }}
          />
        </TabsContent>
      </Tabs>
    </div>
  )
}
 
