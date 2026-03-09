 "use client"
 
 import { useEffect, useMemo, useState } from "react"
 import { Button } from "@/components/ui/button"
 import { PageHeader } from "@/components/ui/page-header"
 import { DataTablePro } from "@/components/ui-kit/DataTablePro"
 import { getStockSummary, StockSummary } from "@/lib/reports"
 
 export default function StockSummaryPage() {
   const [rows, setRows] = useState<StockSummary[]>([])
   const [loading, setLoading] = useState(true)
   const [error, setError] = useState<string | null>(null)
   const [query, setQuery] = useState("")
 
   async function load() {
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
   }
 
   useEffect(() => {
     load()
   }, [])
 
   const filtered = useMemo(() => {
     const q = query.trim().toLowerCase()
     return rows.filter((r) => {
       const matchesText =
         !q ||
         r.itemName.toLowerCase().includes(q) ||
         r.itemCode.toLowerCase().includes(q) ||
         r.warehouseName.toLowerCase().includes(q)
       return matchesText
     })
   }, [query, rows])
 
   return (
     <div className="space-y-6">
       <PageHeader
         title="Stock Summary"
         description="Current stock by item and warehouse."
       />
 
       {error ? <div className="text-sm text-destructive">{error}</div> : null}
 
       <DataTablePro
         columns={[
           { key: "itemName", header: "Item" },
           { key: "itemCode", header: "Code" },
           { key: "warehouseName", header: "Warehouse" },
           { key: "currentStock", header: "Stock" },
           { key: "uom", header: "UOM" },
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
 
