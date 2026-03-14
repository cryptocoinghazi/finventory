 "use client"
 
import Link from "next/link"
import { useEffect, useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import { deleteItem, listItems, Item } from "@/lib/items"
import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import { ConfirmDialog } from "@/components/ui-kit/ConfirmDialog"
import { MoneyText } from "@/components/ui-kit/MoneyText"
import { ItemUploadDialog } from "@/components/items/ItemUploadDialog"
import { Input } from "@/components/ui/input"
import { ChevronDown, ChevronRight, Search } from "lucide-react"
import { useDebounce } from "@/hooks/use-debounce"
import { API_BASE } from "@/lib/api"
import { LabelPrintDialog } from "@/components/items/LabelPrintDialog"

export default function ItemsListPage() {
  const [rows, setRows] = useState<Item[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState("")
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set())
  const [printOpen, setPrintOpen] = useState(false)
  const [collapsedByVendor, setCollapsedByVendor] = useState<Record<string, boolean>>({})
  const [pageByVendor, setPageByVendor] = useState<Record<string, number>>({})
  const debouncedQuery = useDebounce(query, 300)
  const pageSize = 10

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const data = await listItems()
      setRows(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load items")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const filtered = useMemo(() => {
    const q = debouncedQuery.trim().toLowerCase()
    return rows.filter((p) => {
      const matchesText =
        !q ||
        p.name.toLowerCase().includes(q) ||
        p.code.toLowerCase().includes(q) ||
        (p.barcode ?? "").toLowerCase().includes(q) ||
        (p.hsnCode ?? "").toLowerCase().includes(q) ||
        String(p.taxRate).includes(q)
      return matchesText
    })
  }, [debouncedQuery, rows])

  const grouped = useMemo(() => {
    const groups: Record<string, Item[]> = {}
    filtered.forEach((item) => {
      const vendorName = item.vendorName || "Unassigned"
      if (!groups[vendorName]) {
        groups[vendorName] = []
      }
      groups[vendorName].push(item)
    })
    return groups
  }, [filtered])

  const sortedVendorNames = useMemo(() => {
    return Object.keys(grouped).sort()
  }, [grouped])

  useEffect(() => {
    setPageByVendor((prev) => {
      if (sortedVendorNames.length === 0) return prev
      const next: Record<string, number> = {}
      sortedVendorNames.forEach((name) => {
        next[name] = prev[name] ?? 1
      })
      return next
    })
  }, [sortedVendorNames])

  function renderActions(it: Item) {
    return (
      <div className="flex items-center gap-2">
        <Link href={`/masters/items/${it.id}/edit`}>
          <Button size="sm" variant="secondary">Edit</Button>
        </Link>
        <ConfirmDialog
          title={`Delete ${it.name}?`}
          description="This action cannot be undone."
          confirmText="Delete"
          onConfirm={async () => {
            try {
              await deleteItem(it.id)
              setRows((prev) => prev.filter((x) => x.id !== it.id))
              setSelectedIds((prev) => {
                const next = new Set(prev)
                next.delete(it.id)
                return next
              })
            } catch (err) {
              setError(err instanceof Error ? err.message : "Delete failed")
            }
          }}
        />
      </div>
    )
  }

  const selectedItems = useMemo(
    () => rows.filter((r) => selectedIds.has(r.id)),
    [rows, selectedIds]
  )

  function columnsFor(visible: Item[]) {
    const allVisibleSelected = visible.length > 0 && visible.every((r) => selectedIds.has(r.id))
    const someVisibleSelected = visible.some((r) => selectedIds.has(r.id))
    return [
      {
        key: "select",
        header: (
          <input
            aria-label="Select all"
            type="checkbox"
            checked={allVisibleSelected}
            ref={(el) => {
              if (!el) return
              el.indeterminate = !allVisibleSelected && someVisibleSelected
            }}
            onChange={(e) => {
              const checked = e.target.checked
              setSelectedIds((prev) => {
                const next = new Set(prev)
                visible.forEach((it) => {
                  if (checked) next.add(it.id)
                  else next.delete(it.id)
                })
                return next
              })
            }}
            onClick={(e) => e.stopPropagation()}
          />
        ),
        cell: (row: Item) => (
          <input
            aria-label={`Select ${row.name}`}
            type="checkbox"
            checked={selectedIds.has(row.id)}
            onChange={(e) => {
              const checked = e.target.checked
              setSelectedIds((prev) => {
                const next = new Set(prev)
                if (checked) next.add(row.id)
                else next.delete(row.id)
                return next
              })
            }}
            onClick={(e) => e.stopPropagation()}
          />
        ),
        className: "w-[40px]",
      },
      {
        key: "image",
        header: "Image",
        cell: (row: Item) =>
          row.imageUrl ? (
            <div className="h-10 w-10 rounded-md overflow-hidden border bg-muted">
              <img
                src={row.imageUrl.startsWith("http") ? row.imageUrl : `${API_BASE}${row.imageUrl}`}
                alt={row.name}
                className="h-full w-full object-cover"
              />
            </div>
          ) : (
            <div className="h-10 w-10 rounded-md border bg-muted" />
          ),
        className: "w-[70px]",
      },
      { key: "name", header: "Name", sortable: true },
      { key: "code", header: "Code", sortable: true },
      { key: "barcode", header: "Barcode" },
      { key: "hsnCode", header: "HSN" },
      { key: "uom", header: "UOM" },
      {
        key: "unitPrice",
        header: "Unit Price",
        cell: (row: Item) => <MoneyText value={row.unitPrice} />,
        className: "text-right",
      },
      {
        key: "cogs",
        header: "COGS",
        cell: (row: Item) => <MoneyText value={row.cogs} />,
        className: "text-right",
      },
      {
        key: "taxRate",
        header: "Tax %",
        cell: (row: Item) => `${row.taxRate}%`,
        className: "text-right",
      },
      { key: "actions", header: "Actions", cell: renderActions, className: "w-[150px]" },
    ]
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Items"
        description="Manage inventory items grouped by vendor."
        actions={
          <div className="flex gap-2">
            <Button
              variant="secondary"
              disabled={selectedItems.length === 0}
              onClick={() => setPrintOpen(true)}
            >
              Print Labels{selectedItems.length ? ` (${selectedItems.length})` : ""}
            </Button>
            <Button
              variant="outline"
              disabled={selectedIds.size === 0}
              onClick={() => setSelectedIds(new Set())}
            >
              Clear Selection
            </Button>
            <ItemUploadDialog onUpload={load} />
            <Link href="/masters/items/new">
              <Button>Add Item</Button>
            </Link>
          </div>
        }
      />

      {error ? <div className="text-sm text-destructive">{error}</div> : null}

      <div className="relative w-full max-w-sm">
        <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
        <Input
          type="search"
          placeholder="Search items..."
          className="pl-8"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>

      {loading ? (
        <DataTablePro columns={columnsFor([])} data={[]} loading={true} />
      ) : sortedVendorNames.length === 0 ? (
        <DataTablePro 
          columns={columnsFor([])} 
          data={[]} 
          empty={{ 
            title: "No items found", 
            description: "Get started by adding a new item.",
            onAdd: () => {} // handled by header button
          }} 
        />
      ) : (
        sortedVendorNames.map((vendorName) => (
          <div key={vendorName} className="space-y-4 border rounded-lg p-4 bg-card">
            <div className="flex items-center justify-between gap-3">
              <button
                type="button"
                className="text-lg font-semibold inline-flex items-center gap-2"
                onClick={() =>
                  setCollapsedByVendor((prev) => ({
                    ...prev,
                    [vendorName]: !(prev[vendorName] ?? false),
                  }))
                }
              >
                {(collapsedByVendor[vendorName] ?? false) ? (
                  <ChevronRight className="h-5 w-5 text-muted-foreground" />
                ) : (
                  <ChevronDown className="h-5 w-5 text-muted-foreground" />
                )}
                <span>{vendorName}</span>
                <span className="text-sm font-normal text-muted-foreground">
                  ({grouped[vendorName].length})
                </span>
              </button>
            </div>

            {(collapsedByVendor[vendorName] ?? false) ? null : (
              (() => {
                const total = grouped[vendorName].length
                const totalPages = Math.max(1, Math.ceil(total / pageSize))
                const page = Math.min(pageByVendor[vendorName] ?? 1, totalPages)
                const start = (page - 1) * pageSize
                const visible = grouped[vendorName].slice(start, start + pageSize)
                return (
                  <DataTablePro
                    columns={columnsFor(visible)}
                    data={visible}
                    loading={false}
                    page={page}
                    pageSize={pageSize}
                    total={total}
                    onPageChange={(nextPage) =>
                      setPageByVendor((prev) => ({ ...prev, [vendorName]: nextPage }))
                    }
                  />
                )
              })()
            )}
          </div>
        ))
      )}

      <LabelPrintDialog
        open={printOpen}
        onOpenChange={setPrintOpen}
        selectedItems={selectedItems}
      />
    </div>
  )
}
 
