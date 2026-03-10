 "use client"
 
import { useCallback, useEffect, useMemo, useState } from "react"
 import { Button } from "@/components/ui/button"
 import { PageHeader } from "@/components/ui/page-header"
 import { DataTablePro } from "@/components/ui-kit/DataTablePro"
 import { getPartyOutstanding, PartyOutstanding } from "@/lib/reports"
import { ChevronDown, ChevronRight } from "lucide-react"
 
 export default function PartyOutstandingPage() {
   const [rows, setRows] = useState<PartyOutstanding[]>([])
   const [loading, setLoading] = useState(true)
   const [error, setError] = useState<string | null>(null)
   const [query, setQuery] = useState("")
  const [vendorsCollapsed, setVendorsCollapsed] = useState(false)
  const [customersCollapsed, setCustomersCollapsed] = useState(false)
 
  const load = useCallback(async () => {
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
  }, [])
 
   useEffect(() => {
     load()
  }, [load])
 
  const filteredByQuery = useMemo(() => {
     const q = query.trim().toLowerCase()
     return rows.filter((r) => {
      return !q || r.partyName.toLowerCase().includes(q)
     })
  }, [query, rows])

  const vendorRows = useMemo(
    () => filteredByQuery.filter((r) => r.partyType === "VENDOR"),
    [filteredByQuery]
  )

  const customerRows = useMemo(
    () => filteredByQuery.filter((r) => r.partyType === "CUSTOMER"),
    [filteredByQuery]
  )

  const vendorTotals = useMemo(() => {
    return vendorRows.reduce(
      (acc, r) => {
        acc.receivable += r.totalReceivable || 0
        acc.payable += r.totalPayable || 0
        acc.net += r.netBalance || 0
        return acc
      },
      { receivable: 0, payable: 0, net: 0 }
    )
  }, [vendorRows])

  const customerTotals = useMemo(() => {
    return customerRows.reduce(
      (acc, r) => {
        acc.receivable += r.totalReceivable || 0
        acc.payable += r.totalPayable || 0
        acc.net += r.netBalance || 0
        return acc
      },
      { receivable: 0, payable: 0, net: 0 }
    )
  }, [customerRows])
 
   return (
     <div className="space-y-6">
       <PageHeader
         title="Party Outstanding"
         description="Receivables and payables by party."
       />
 
       {error ? <div className="text-sm text-destructive">{error}</div> : null}
      <div className="flex flex-wrap items-center gap-2">
        <input
          className="w-full max-w-xs px-3 py-2 rounded-md border border-input bg-background"
          placeholder="Search by party name"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <Button variant="outline" onClick={load} disabled={loading}>
          Refresh
        </Button>
      </div>

      <div className="space-y-4">
        <div className="rounded-2xl border border-border overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3 bg-muted/30">
            <button
              type="button"
              className="inline-flex items-center gap-2 font-semibold"
              onClick={() => setVendorsCollapsed((v) => !v)}
            >
              {vendorsCollapsed ? (
                <ChevronRight className="h-4 w-4" />
              ) : (
                <ChevronDown className="h-4 w-4" />
              )}
              Vendors
            </button>
            <div className="text-sm text-muted-foreground">
              Receivable: {vendorTotals.receivable.toFixed(2)} • Payable: {vendorTotals.payable.toFixed(2)} • Net:{" "}
              {vendorTotals.net.toFixed(2)}
            </div>
          </div>
          <div className={vendorsCollapsed ? "hidden" : "block"}>
            <DataTablePro
              columns={[
                { key: "partyName", header: "Party", sortable: true },
                { key: "totalReceivable", header: "Receivable", sortable: true },
                { key: "totalPayable", header: "Payable", sortable: true },
                { key: "netBalance", header: "Net", sortable: true },
              ]}
              data={vendorRows}
              loading={loading}
              actions={null}
              empty={{
                title: "No vendor outstanding",
                description: "No vendors match the current filters",
              }}
            />
          </div>
        </div>

        <div className="rounded-2xl border border-border overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3 bg-muted/30">
            <button
              type="button"
              className="inline-flex items-center gap-2 font-semibold"
              onClick={() => setCustomersCollapsed((v) => !v)}
            >
              {customersCollapsed ? (
                <ChevronRight className="h-4 w-4" />
              ) : (
                <ChevronDown className="h-4 w-4" />
              )}
              Customers
            </button>
            <div className="text-sm text-muted-foreground">
              Receivable: {customerTotals.receivable.toFixed(2)} • Payable: {customerTotals.payable.toFixed(2)} • Net:{" "}
              {customerTotals.net.toFixed(2)}
            </div>
          </div>
          <div className={customersCollapsed ? "hidden" : "block"}>
            <DataTablePro
              columns={[
                { key: "partyName", header: "Party", sortable: true },
                { key: "totalReceivable", header: "Receivable", sortable: true },
                { key: "totalPayable", header: "Payable", sortable: true },
                { key: "netBalance", header: "Net", sortable: true },
              ]}
              data={customerRows}
              loading={loading}
              actions={null}
              empty={{
                title: "No customer outstanding",
                description: "No customers match the current filters",
              }}
            />
          </div>
        </div>
      </div>
     </div>
   )
 }
 
