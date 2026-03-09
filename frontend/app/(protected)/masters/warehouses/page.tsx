 "use client"
 
 import Link from "next/link"
 import { useEffect, useMemo, useState } from "react"
 import { Button } from "@/components/ui/button"
 import { PageHeader } from "@/components/ui/page-header"
 import { deleteWarehouse, listWarehouses, Warehouse } from "@/lib/warehouses"
 import { DataTablePro } from "@/components/ui-kit/DataTablePro"
 import { ConfirmDialog } from "@/components/ui-kit/ConfirmDialog"
 
 export default function WarehousesListPage() {
   const [rows, setRows] = useState<Warehouse[]>([])
   const [loading, setLoading] = useState(true)
   const [error, setError] = useState<string | null>(null)
   const [query, setQuery] = useState("")
 
   async function load() {
     setLoading(true)
     setError(null)
     try {
      const data = await listWarehouses()
      setRows(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load warehouses")
    } finally {
       setLoading(false)
     }
   }
 
   useEffect(() => {
     load()
   }, [])
 
   const filtered = useMemo(() => {
     const q = query.trim().toLowerCase()
     return rows.filter((w) => {
       const matchesText =
         !q ||
         w.name.toLowerCase().includes(q) ||
         (w.stateCode ?? "").toLowerCase().includes(q) ||
         (w.location ?? "").toLowerCase().includes(q)
       return matchesText
     })
   }, [query, rows])
 
   function renderActions(w: Warehouse) {
     return (
       <div className="flex items-center gap-2">
         <ConfirmDialog
           title={`Delete ${w.name}?`}
           description="This action cannot be undone."
           confirmText="Delete"
           onConfirm={async () => {
             try {
               await deleteWarehouse(w.id)
              setRows((prev) => prev.filter((x) => x.id !== w.id))
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
         title="Warehouses"
         description="Manage storage locations."
         actions={
           <Link href="/masters/warehouses/new">
             <Button>Add Warehouse</Button>
           </Link>
         }
       />
 
       {error ? <div className="text-sm text-destructive">{error}</div> : null}
 
       <DataTablePro
         columns={[
           { key: "name", header: "Name" },
           { key: "stateCode", header: "State" },
           { key: "location", header: "Location" },
           { key: "actions", header: "Actions", cell: renderActions },
         ]}
         data={filtered}
         loading={loading}
         filters={
           <div className="flex flex-wrap items-center gap-2">
             <input
               className="w-full max-w-xs px-3 py-2 rounded-md border border-input bg-background"
               placeholder="Search name/state/location"
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
           title: "No warehouses found",
           description: "Create your first warehouse",
           onAdd: () => {
             window.location.href = "/masters/warehouses/new"
           },
         }}
       />
     </div>
   )
 }
 
