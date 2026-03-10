 "use client"
 
 import Link from "next/link"
import { useCallback, useEffect, useMemo, useState } from "react"
 import { Button } from "@/components/ui/button"
 import { PageHeader } from "@/components/ui/page-header"
import { InvoicePaymentStatus, listSalesInvoices, SalesInvoice } from "@/lib/sales-invoices"
 import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
 
 export default function SalesInvoicesListPage() {
   const [rows, setRows] = useState<SalesInvoice[]>([])
   const [loading, setLoading] = useState(true)
   const [error, setError] = useState<string | null>(null)
   const [query, setQuery] = useState("")
  const [paymentStatus, setPaymentStatus] = useState<InvoicePaymentStatus | "">("")
  const [fromDate, setFromDate] = useState("")
  const [toDate, setToDate] = useState("")
 
  const load = useCallback(async () => {
     setLoading(true)
     setError(null)
     try {
      const data = await listSalesInvoices({ paymentStatus, fromDate, toDate })
      setRows(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load sales invoices")
    } finally {
      setLoading(false)
    }
  }, [fromDate, paymentStatus, toDate])
 
   useEffect(() => {
     load()
  }, [load])
 
   const filtered = useMemo(() => {
     const q = query.trim().toLowerCase()
     return rows.filter((inv) => {
       const matchesText =
         !q ||
         (inv.invoiceNumber ?? "").toLowerCase().includes(q)
       return matchesText
     })
   }, [query, rows])
 
   return (
     <div className="space-y-6">
       <PageHeader
         title="Sales Invoices"
         description="Track sales invoices."
         actions={
           <Link href="/sales/invoices/new">
             <Button>Create Invoice</Button>
           </Link>
         }
       />
 
       {error ? <div className="text-sm text-destructive">{error}</div> : null}
 
       <DataTablePro
        columns={[
          { key: "invoiceDate", header: "Date" },
          { key: "invoiceNumber", header: "Number", cell: (row) => (
            <Link href={`/sales/invoices/${row.id}`} className="text-primary hover:underline">
              {row.invoiceNumber || "Draft"}
            </Link>
          )},
          { key: "partyName", header: "Customer", cell: (row) => row.partyName || "N/A" },
          { key: "warehouseName", header: "Warehouse", cell: (row) => row.warehouseName || "N/A" },
          { key: "paymentStatus", header: "Status", cell: (row) => row.paymentStatus || "PENDING" },
          { key: "grandTotal", header: "Total", cell: (row) => row.grandTotal?.toFixed(2) || "0.00" },
        ]}
         data={filtered}
         loading={loading}
         filters={
           <div className="flex flex-wrap items-center gap-2">
             <input
               className="w-full max-w-xs px-3 py-2 rounded-md border border-input bg-background"
               placeholder="Search by invoice number"
               value={query}
               onChange={(e) => setQuery(e.target.value)}
             />
            <div className="w-full max-w-[180px]">
              <Select value={paymentStatus} onValueChange={(v) => setPaymentStatus(v as InvoicePaymentStatus | "")}>
                <SelectTrigger>
                  <SelectValue placeholder="All Status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="">All</SelectItem>
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
           title: "No invoices found",
           description: "Create your first invoice",
           onAdd: () => {
             window.location.href = "/sales/invoices/new"
           },
         }}
       />
     </div>
   )
 }
 
