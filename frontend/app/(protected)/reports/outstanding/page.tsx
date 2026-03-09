 "use client"
 
 import { useEffect, useMemo, useState } from "react"
 import { Button } from "@/components/ui/button"
 import { PageHeader } from "@/components/ui/page-header"
 import { DataTablePro } from "@/components/ui-kit/DataTablePro"
 import { getPartyOutstanding, PartyOutstanding } from "@/lib/reports"
 
 export default function PartyOutstandingPage() {
   const [rows, setRows] = useState<PartyOutstanding[]>([])
   const [loading, setLoading] = useState(true)
   const [error, setError] = useState<string | null>(null)
   const [query, setQuery] = useState("")
   const [type, setType] = useState<"" | "CUSTOMER" | "VENDOR">("")
 
   async function load() {
     setLoading(true)
     setError(null)
     try {
      const data = await getPartyOutstanding()
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
         !q || r.partyName.toLowerCase().includes(q)
       const matchesType = !type || r.partyType === type
       return matchesText && matchesType
     })
   }, [query, rows, type])
 
   return (
     <div className="space-y-6">
       <PageHeader
         title="Party Outstanding"
         description="Receivables and payables by party."
       />
 
       {error ? <div className="text-sm text-destructive">{error}</div> : null}
 
       <DataTablePro
         columns={[
           { key: "partyName", header: "Party" },
           { key: "partyType", header: "Type" },
           { key: "totalReceivable", header: "Receivable" },
           { key: "totalPayable", header: "Payable" },
           { key: "netBalance", header: "Net" },
         ]}
         data={filtered}
         loading={loading}
         filters={
           <div className="flex flex-wrap items-center gap-2">
             <input
               className="w-full max-w-xs px-3 py-2 rounded-md border border-input bg-background"
               placeholder="Search by party name"
               value={query}
               onChange={(e) => setQuery(e.target.value)}
             />
             <select
               className="px-3 py-2 rounded-md border border-input bg-background"
               value={type}
               onChange={(e) =>
                 setType(e.target.value as "" | "CUSTOMER" | "VENDOR")
               }
             >
               <option value="">All Types</option>
               <option value="CUSTOMER">Customer</option>
               <option value="VENDOR">Vendor</option>
             </select>
             <Button variant="outline" onClick={load} disabled={loading}>
               Refresh
             </Button>
           </div>
         }
         actions={null}
         empty={{
           title: "No outstanding data",
           description: "Try adjusting filters",
         }}
       />
     </div>
   )
 }
 
