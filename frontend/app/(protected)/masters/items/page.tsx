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
import { Search } from "lucide-react"
import { useDebounce } from "@/hooks/use-debounce"

export default function ItemsListPage() {
  const [rows, setRows] = useState<Item[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState("")
  const debouncedQuery = useDebounce(query, 300)

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
        (p.hsnCode ?? "").toLowerCase().includes(q) ||
        String(p.taxRate).includes(q)
      return matchesText
    })
  }, [debouncedQuery, rows])

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
            } catch (err) {
              setError(err instanceof Error ? err.message : "Delete failed")
            }
          }}
        />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Items"
        description="Manage inventory items."
        actions={
          <div className="flex gap-2">
            <ItemUploadDialog onUpload={load} />
            <Link href="/masters/items/new">
              <Button>Add Item</Button>
            </Link>
          </div>
        }
      />

      {error ? <div className="text-sm text-destructive">{error}</div> : null}

      <DataTablePro
        columns={[
          { key: "name", header: "Name", sortable: true },
          { key: "code", header: "Code", sortable: true },
          { key: "hsnCode", header: "HSN" },
          { key: "uom", header: "UOM" },
          { 
            key: "unitPrice", 
            header: "Unit Price", 
            cell: (row) => <MoneyText value={row.unitPrice} />,
            className: "text-right"
          },
          { 
            key: "taxRate", 
            header: "Tax %", 
            cell: (row) => `${row.taxRate}%`,
            className: "text-right"
          },
          { key: "actions", header: "Actions", cell: renderActions, className: "w-[150px]" },
        ]}
        data={filtered}
        loading={loading}
        filters={
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
        }
      />
    </div>
  )
}
 
