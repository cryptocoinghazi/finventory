 "use client"
 
 import Link from "next/link"
 import { useEffect, useMemo, useState } from "react"
 import { Button } from "@/components/ui/button"
 import { PageHeader } from "@/components/ui/page-header"
 import { deleteItem, listItems, Item } from "@/lib/items"
 import { DataTablePro } from "@/components/ui-kit/DataTablePro"
 import { ConfirmDialog } from "@/components/ui-kit/ConfirmDialog"
 
 export default function ItemsListPage() {
   const [rows, setRows] = useState<Item[]>([])
   const [loading, setLoading] = useState(true)
   const [error, setError] = useState<string | null>(null)
   const [query, setQuery] = useState("")
 
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
     const q = query.trim().toLowerCase()
     return rows.filter((p) => {
       const matchesText =
         !q ||
         p.name.toLowerCase().includes(q) ||
         p.code.toLowerCase().includes(q) ||
         (p.hsnCode ?? "").toLowerCase().includes(q) ||
         String(p.taxRate).includes(q)
       return matchesText
     })
   }, [query, rows])
 
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
           <Link href="/masters/items/new">
             <Button>Add Item</Button>
           </Link>
         }
       />
 
       {error ? <div className="text-sm text-destructive">{error}</div> : null}
 
       <DataTablePro
         columns={[
           { key: "name", header: "Name" },
           { key: "code", header: "Code" },
           { key: "hsnCode", header: "HSN" },
           { key: "uom", header: "UOM" },
           { key: "unitPrice", header: "Unit Price" },
           { key: "taxRate", header: "Tax %" },
           { key: "actions", header: "Actions", cell: renderActions },
         ]}
         data={filtered}
         loading={loading}
         filters={
           <div className="flex flex-wrap items-center gap-2">
             <input
               className="w-full max-w-xs px-3 py-2 rounded-md border border-input bg-background"
               placeholder="Search name/code/HSN"
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
           title: "No items found",
           description: "Create your first item",
           onAdd: () => {
             window.location.href = "/masters/items/new"
           },
         }}
       />
     </div>
   )
 }
 
