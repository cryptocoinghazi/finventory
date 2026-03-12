 "use client"
 
import { useCallback, useEffect, useMemo, useState } from "react"
 import { Button } from "@/components/ui/button"
 import { PageHeader } from "@/components/ui/page-header"
 import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import { getPartyOutstanding, getPartyOutstandingLedger, PartyLedgerEntry, PartyOutstanding } from "@/lib/reports"
import { ChevronDown, ChevronRight } from "lucide-react"
import { StatCard } from "@/components/ui-kit/StatCard"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { format } from "date-fns"
 
 export default function PartyOutstandingPage() {
   const [rows, setRows] = useState<PartyOutstanding[]>([])
   const [loading, setLoading] = useState(true)
   const [error, setError] = useState<string | null>(null)
   const [query, setQuery] = useState("")
  const [vendorsCollapsed, setVendorsCollapsed] = useState(false)
  const [customersCollapsed, setCustomersCollapsed] = useState(false)
  const [fromDate, setFromDate] = useState("")
  const [toDate, setToDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [partyType, setPartyType] = useState<"__ALL__" | "CUSTOMER" | "VENDOR">("__ALL__")
  const [minOutstanding, setMinOutstanding] = useState("")

  const [ledgerOpen, setLedgerOpen] = useState(false)
  const [ledgerParty, setLedgerParty] = useState<PartyOutstanding | null>(null)
  const [ledgerRows, setLedgerRows] = useState<PartyLedgerEntry[]>([])
  const [ledgerLoading, setLedgerLoading] = useState(false)
  const [ledgerError, setLedgerError] = useState<string | null>(null)
 
  const load = useCallback(async () => {
     setLoading(true)
     setError(null)
     try {
      const min = minOutstanding.trim()
      const minValue = min ? Number(min) : undefined
      const data = await getPartyOutstanding({
        fromDate: fromDate || undefined,
        toDate: toDate || undefined,
        partyType: partyType === "__ALL__" ? undefined : partyType,
        minOutstanding: minValue,
      })
      setRows(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load report")
    } finally {
      setLoading(false)
     }
  }, [fromDate, minOutstanding, partyType, toDate])
 
   useEffect(() => {
     load()
  }, [load])
 
  const filteredByQuery = useMemo(() => {
     const q = query.trim().toLowerCase()
     return rows.filter((r) => {
      if (!q) return true
      return (
        r.partyName.toLowerCase().includes(q) ||
        (r.phone || "").toLowerCase().includes(q) ||
        (r.gstin || "").toLowerCase().includes(q)
      )
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

  const allTotals = useMemo(() => {
    return filteredByQuery.reduce(
      (acc, r) => {
        acc.receivable += r.totalReceivable || 0
        acc.payable += r.totalPayable || 0
        acc.net += r.netBalance || 0
        return acc
      },
      { receivable: 0, payable: 0, net: 0 }
    )
  }, [filteredByQuery])

  const openLedger = useCallback(
    async (party: PartyOutstanding) => {
      setLedgerParty(party)
      setLedgerOpen(true)
      setLedgerRows([])
      setLedgerError(null)
      setLedgerLoading(true)
      try {
        const data = await getPartyOutstandingLedger({
          partyId: party.partyId,
          fromDate: fromDate || undefined,
          toDate: toDate || undefined,
        })
        setLedgerRows(data)
      } catch (err) {
        setLedgerError(err instanceof Error ? err.message : "Failed to load party ledger")
      } finally {
        setLedgerLoading(false)
      }
    },
    [fromDate, toDate]
  )

  const exportCsv = useCallback(() => {
    const headers = [
      "Type",
      "Party",
      "Phone",
      "GSTIN",
      "Receivable",
      "Payable",
      "Net",
      "0-30",
      "31-60",
      "61-90",
      "90+",
    ]
    const lines = [headers.join(",")]
    for (const r of filteredByQuery) {
      const row = [
        r.partyType,
        r.partyName,
        r.phone || "",
        r.gstin || "",
        String(r.totalReceivable || 0),
        String(r.totalPayable || 0),
        String(r.netBalance || 0),
        String(r.age0to30 || 0),
        String(r.age31to60 || 0),
        String(r.age61to90 || 0),
        String(r.age90Plus || 0),
      ].map((v) => `"${String(v).replaceAll('"', '""')}"`)
      lines.push(row.join(","))
    }

    const blob = new Blob([lines.join("\n")], { type: "text/csv;charset=utf-8" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = `party-outstanding-${toDate || new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  }, [filteredByQuery, toDate])

  const exportPdf = useCallback(() => {
    const title = `Party Outstanding (${fromDate || "All"} to ${toDate || "-"})`
    const doc = window.open("", "_blank")
    if (!doc) return

    const escapeHtml = (s: string) =>
      s
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;")

    const rowsHtml = filteredByQuery
      .map((r) => {
        return `<tr>
          <td>${escapeHtml(r.partyType)}</td>
          <td>${escapeHtml(r.partyName)}</td>
          <td>${escapeHtml(r.phone || "")}</td>
          <td>${escapeHtml(r.gstin || "")}</td>
          <td style="text-align:right">${Number(r.totalReceivable || 0).toFixed(2)}</td>
          <td style="text-align:right">${Number(r.totalPayable || 0).toFixed(2)}</td>
          <td style="text-align:right">${Number(r.netBalance || 0).toFixed(2)}</td>
          <td style="text-align:right">${Number(r.age0to30 || 0).toFixed(2)}</td>
          <td style="text-align:right">${Number(r.age31to60 || 0).toFixed(2)}</td>
          <td style="text-align:right">${Number(r.age61to90 || 0).toFixed(2)}</td>
          <td style="text-align:right">${Number(r.age90Plus || 0).toFixed(2)}</td>
        </tr>`
      })
      .join("")

    doc.document.write(`<!doctype html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>${escapeHtml(title)}</title>
    <style>
      body { font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial; padding: 24px; }
      h1 { font-size: 18px; margin: 0 0 8px; }
      .meta { font-size: 12px; color: #555; margin-bottom: 16px; }
      table { width: 100%; border-collapse: collapse; font-size: 12px; }
      th, td { border: 1px solid #ddd; padding: 8px; vertical-align: top; }
      th { background: #f5f5f5; text-align: left; }
    </style>
  </head>
  <body>
    <h1>${escapeHtml(title)}</h1>
    <div class="meta">Generated: ${escapeHtml(new Date().toLocaleString())}</div>
    <table>
      <thead>
        <tr>
          <th>Type</th>
          <th>Party</th>
          <th>Phone</th>
          <th>GSTIN</th>
          <th style="text-align:right">Receivable</th>
          <th style="text-align:right">Payable</th>
          <th style="text-align:right">Net</th>
          <th style="text-align:right">0-30</th>
          <th style="text-align:right">31-60</th>
          <th style="text-align:right">61-90</th>
          <th style="text-align:right">90+</th>
        </tr>
      </thead>
      <tbody>
        ${rowsHtml}
      </tbody>
    </table>
  </body>
</html>`)
    doc.document.close()
    doc.focus()
    doc.print()
    doc.close()
  }, [filteredByQuery, fromDate, toDate])
 
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
          placeholder="Search name / phone / GSTIN"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <div className="w-full max-w-[200px]">
          <Select value={partyType} onValueChange={(v) => setPartyType(v as typeof partyType)}>
            <SelectTrigger>
              <SelectValue placeholder="All parties" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="__ALL__">All</SelectItem>
              <SelectItem value="CUSTOMER">Customers</SelectItem>
              <SelectItem value="VENDOR">Vendors</SelectItem>
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
        <input
          className="w-full max-w-[170px] px-3 py-2 rounded-md border border-input bg-background"
          placeholder="Min outstanding"
          inputMode="decimal"
          value={minOutstanding}
          onChange={(e) => setMinOutstanding(e.target.value)}
        />
        <Button variant="outline" onClick={load} disabled={loading}>
          Refresh
        </Button>
        <Button variant="outline" onClick={exportCsv} disabled={loading || filteredByQuery.length === 0}>
          Export CSV
        </Button>
        <Button variant="outline" onClick={exportPdf} disabled={loading || filteredByQuery.length === 0}>
          Export PDF
        </Button>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <StatCard title="Total Receivable" loading={loading} value={`₹${allTotals.receivable.toFixed(2)}`} />
        <StatCard title="Total Payable" loading={loading} value={`₹${allTotals.payable.toFixed(2)}`} />
        <StatCard title="Net" loading={loading} value={`₹${allTotals.net.toFixed(2)}`} />
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
                {
                  key: "partyName",
                  header: "Party",
                  sortable: true,
                  cell: (row) => (
                    <button
                      type="button"
                      className="text-left hover:underline"
                      onClick={() => openLedger(row)}
                    >
                      <div className="font-medium">{row.partyName}</div>
                      <div className="text-xs text-muted-foreground">
                        {[row.phone, row.gstin].filter(Boolean).join(" • ")}
                      </div>
                    </button>
                  ),
                },
                {
                  key: "totalPayable",
                  header: "Payable",
                  sortable: true,
                  cell: (row) => `₹${(row.totalPayable || 0).toFixed(2)}`,
                },
                {
                  key: "age0to30",
                  header: "0–30",
                  sortable: true,
                  cell: (row) => `₹${(Math.max(0, -(row.age0to30 || 0)) || 0).toFixed(2)}`,
                },
                {
                  key: "age31to60",
                  header: "31–60",
                  sortable: true,
                  cell: (row) => `₹${(Math.max(0, -(row.age31to60 || 0)) || 0).toFixed(2)}`,
                },
                {
                  key: "age61to90",
                  header: "61–90",
                  sortable: true,
                  cell: (row) => `₹${(Math.max(0, -(row.age61to90 || 0)) || 0).toFixed(2)}`,
                },
                {
                  key: "age90Plus",
                  header: "90+",
                  sortable: true,
                  cell: (row) => `₹${(Math.max(0, -(row.age90Plus || 0)) || 0).toFixed(2)}`,
                },
                {
                  key: "netBalance",
                  header: "Net",
                  sortable: true,
                  cell: (row) => `₹${(row.netBalance || 0).toFixed(2)}`,
                },
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
                {
                  key: "partyName",
                  header: "Party",
                  sortable: true,
                  cell: (row) => (
                    <button
                      type="button"
                      className="text-left hover:underline"
                      onClick={() => openLedger(row)}
                    >
                      <div className="font-medium">{row.partyName}</div>
                      <div className="text-xs text-muted-foreground">
                        {[row.phone, row.gstin].filter(Boolean).join(" • ")}
                      </div>
                    </button>
                  ),
                },
                {
                  key: "totalReceivable",
                  header: "Receivable",
                  sortable: true,
                  cell: (row) => `₹${(row.totalReceivable || 0).toFixed(2)}`,
                },
                {
                  key: "age0to30",
                  header: "0–30",
                  sortable: true,
                  cell: (row) => `₹${(Math.max(0, row.age0to30 || 0) || 0).toFixed(2)}`,
                },
                {
                  key: "age31to60",
                  header: "31–60",
                  sortable: true,
                  cell: (row) => `₹${(Math.max(0, row.age31to60 || 0) || 0).toFixed(2)}`,
                },
                {
                  key: "age61to90",
                  header: "61–90",
                  sortable: true,
                  cell: (row) => `₹${(Math.max(0, row.age61to90 || 0) || 0).toFixed(2)}`,
                },
                {
                  key: "age90Plus",
                  header: "90+",
                  sortable: true,
                  cell: (row) => `₹${(Math.max(0, row.age90Plus || 0) || 0).toFixed(2)}`,
                },
                {
                  key: "netBalance",
                  header: "Net",
                  sortable: true,
                  cell: (row) => `₹${(row.netBalance || 0).toFixed(2)}`,
                },
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

      <Dialog open={ledgerOpen} onOpenChange={setLedgerOpen}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>{ledgerParty ? ledgerParty.partyName : "Party Ledger"}</DialogTitle>
          </DialogHeader>
          {ledgerError ? <div className="text-sm text-destructive">{ledgerError}</div> : null}
          <div className="text-sm text-muted-foreground">
            {fromDate || toDate ? (
              <>
                Range: {fromDate || "All"} to {toDate || "-"}
              </>
            ) : null}
          </div>
          <DataTablePro
            columns={[
              {
                key: "date",
                header: "Date",
                sortable: true,
                cell: (row) => format(new Date(row.date), "dd MMM yyyy"),
              },
              { key: "refType", header: "Ref", sortable: true },
              { key: "description", header: "Description" },
              {
                key: "amount",
                header: "Amount",
                sortable: true,
                cell: (row) => `₹${(row.amount || 0).toFixed(2)}`,
              },
            ]}
            data={ledgerRows}
            loading={ledgerLoading}
            actions={null}
            empty={{
              title: "No entries",
              description: "No outstanding-related ledger entries found for this party",
            }}
          />
        </DialogContent>
      </Dialog>
     </div>
   )
 }
 
