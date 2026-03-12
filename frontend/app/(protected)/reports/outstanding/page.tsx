 "use client"
 
import { useCallback, useEffect, useMemo, useRef, useState } from "react"
 import { Button } from "@/components/ui/button"
 import { PageHeader } from "@/components/ui/page-header"
 import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import {
  filterPartyOutstandingRows,
  getPartyOutstandingStatus,
  getPartyOutstanding,
  getPartyOutstandingLedger,
  PartyLedgerEntry,
  PartyOutstanding,
  PartyOutstandingStatusFilter,
} from "@/lib/reports"
import { ChevronDown, ChevronRight } from "lucide-react"
import { StatCard } from "@/components/ui-kit/StatCard"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Dialog, DialogClose, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
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
  const [status, setStatus] = useState<PartyOutstandingStatusFilter>("__ALL__")
  const [minOutstanding, setMinOutstanding] = useState("")

  const [ledgerOpen, setLedgerOpen] = useState(false)
  const [ledgerParty, setLedgerParty] = useState<PartyOutstanding | null>(null)
  const [ledgerRows, setLedgerRows] = useState<PartyLedgerEntry[]>([])
  const [ledgerLoading, setLedgerLoading] = useState(false)
  const [ledgerError, setLedgerError] = useState<string | null>(null)
  const ledgerBodyRef = useRef<HTMLDivElement | null>(null)
  const vendorBodyRef = useRef<HTMLDivElement | null>(null)
  const customerBodyRef = useRef<HTMLDivElement | null>(null)
 
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
 
  type RowWithStatus = PartyOutstanding & { status: ReturnType<typeof getPartyOutstandingStatus> }

  const resetScroll = useCallback(() => {
    vendorBodyRef.current?.scrollTo({ top: 0, behavior: "smooth" })
    customerBodyRef.current?.scrollTo({ top: 0, behavior: "smooth" })
    window.scrollTo({ top: 0, behavior: "smooth" })
  }, [])

   useEffect(() => {
     load()
  }, [load])
 
  const filteredRows = useMemo(
    () => filterPartyOutstandingRows(rows, { query, status }),
    [query, rows, status]
  )

  const vendorRows = useMemo(
    () =>
      filteredRows
        .filter((r) => r.partyType === "VENDOR")
        .map((r) => ({ ...r, status: getPartyOutstandingStatus(r) })),
    [filteredRows]
  )

  const customerRows = useMemo(
    () =>
      filteredRows
        .filter((r) => r.partyType === "CUSTOMER")
        .map((r) => ({ ...r, status: getPartyOutstandingStatus(r) })),
    [filteredRows]
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
    return filteredRows.reduce(
      (acc, r) => {
        acc.receivable += r.totalReceivable || 0
        acc.payable += r.totalPayable || 0
        acc.net += r.netBalance || 0
        return acc
      },
      { receivable: 0, payable: 0, net: 0 }
    )
  }, [filteredRows])

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
      "Status",
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
    for (const r of filteredRows) {
      const row = [
        r.partyType,
        r.partyName,
        getPartyOutstandingStatus(r),
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
  }, [filteredRows, toDate])

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

    const rowsHtml = filteredRows
      .map((r) => {
        return `<tr>
          <td>${escapeHtml(r.partyType)}</td>
          <td>${escapeHtml(r.partyName)}</td>
          <td>${escapeHtml(getPartyOutstandingStatus(r))}</td>
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
          <th>Status</th>
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
  }, [filteredRows, fromDate, toDate])
 
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
          onChange={(e) => {
            setQuery(e.target.value)
            resetScroll()
          }}
        />
        <div className="w-full max-w-[200px]">
          <Select
            value={partyType}
            onValueChange={(v) => {
              setPartyType(v as typeof partyType)
              resetScroll()
            }}
          >
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
        <div className="w-full max-w-[170px]">
          <Select
            value={status}
            onValueChange={(v) => {
              setStatus(v as PartyOutstandingStatusFilter)
              resetScroll()
            }}
          >
            <SelectTrigger>
              <SelectValue placeholder="Status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="__ALL__">All</SelectItem>
              <SelectItem value="UNPAID">Unpaid</SelectItem>
              <SelectItem value="PENDING">Pending</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <input
          className="w-full max-w-[170px] px-3 py-2 rounded-md border border-input bg-background"
          type="date"
          value={fromDate}
          onChange={(e) => {
            setFromDate(e.target.value)
            resetScroll()
          }}
        />
        <input
          className="w-full max-w-[170px] px-3 py-2 rounded-md border border-input bg-background"
          type="date"
          value={toDate}
          onChange={(e) => {
            setToDate(e.target.value)
            resetScroll()
          }}
        />
        <input
          className="w-full max-w-[170px] px-3 py-2 rounded-md border border-input bg-background"
          placeholder="Min outstanding"
          inputMode="decimal"
          value={minOutstanding}
          onChange={(e) => {
            setMinOutstanding(e.target.value)
            resetScroll()
          }}
        />
        <Button variant="outline" onClick={load} disabled={loading}>
          Refresh
        </Button>
        <Button variant="outline" onClick={exportCsv} disabled={loading || filteredRows.length === 0}>
          Export CSV
        </Button>
        <Button variant="outline" onClick={exportPdf} disabled={loading || filteredRows.length === 0}>
          Export PDF
        </Button>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <StatCard title="Total Receivable" loading={loading} value={`₹${allTotals.receivable.toFixed(2)}`} />
        <StatCard title="Total Payable" loading={loading} value={`₹${allTotals.payable.toFixed(2)}`} />
        <StatCard title="Net" loading={loading} value={`₹${allTotals.net.toFixed(2)}`} />
      </div>

      {!loading && !error && filteredRows.length === 0 ? (
        <div className="rounded-2xl border border-border bg-muted/10 px-6 py-5 text-sm text-muted-foreground">
          No parties match the current filters.
        </div>
      ) : null}

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
            <div ref={vendorBodyRef} className="max-w-full overflow-x-auto">
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
                  key: "status",
                  header: "Status",
                  sortable: true,
                  className: "w-[110px]",
                  cell: (row: RowWithStatus) => (
                    <span className={row.status === "UNPAID" ? "text-destructive" : "text-muted-foreground"}>
                      {row.status === "UNPAID" ? "Unpaid" : "Pending"}
                    </span>
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
            <div ref={customerBodyRef} className="max-w-full overflow-x-auto">
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
                  key: "status",
                  header: "Status",
                  sortable: true,
                  className: "w-[110px]",
                  cell: (row: RowWithStatus) => (
                    <span className={row.status === "UNPAID" ? "text-destructive" : "text-muted-foreground"}>
                      {row.status === "UNPAID" ? "Unpaid" : "Pending"}
                    </span>
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
      </div>

      <Dialog open={ledgerOpen} onOpenChange={setLedgerOpen}>
        <DialogContent
          className="max-w-3xl w-[calc(100vw-2rem)] sm:w-full p-0 flex flex-col overflow-hidden"
          onOpenAutoFocus={(e) => {
            e.preventDefault()
            ledgerBodyRef.current?.focus()
          }}
        >
          <div className="p-6 pb-4 border-b border-border">
            <DialogHeader className="space-y-2">
              <DialogTitle>{ledgerParty ? ledgerParty.partyName : "Party Ledger"}</DialogTitle>
              <div className="text-sm text-muted-foreground">
                {fromDate || toDate ? (
                  <>
                    Range: {fromDate || "All"} to {toDate || "-"}
                  </>
                ) : null}
              </div>
              {ledgerError ? <div className="text-sm text-destructive">{ledgerError}</div> : null}
            </DialogHeader>
          </div>

          <div
            ref={ledgerBodyRef}
            tabIndex={0}
            className="max-h-[80vh] overflow-y-auto overflow-x-hidden scroll-smooth px-6 py-4 focus:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 ring-offset-background"
            onKeyDownCapture={(e) => {
              const target = e.target as HTMLElement | null
              const tag = target?.tagName
              if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return
              if (e.altKey || e.ctrlKey || e.metaKey) return

              const el = ledgerBodyRef.current
              if (!el) return

              if (e.key === "ArrowDown") {
                e.preventDefault()
                el.scrollBy({ top: 40, behavior: "smooth" })
                return
              }
              if (e.key === "ArrowUp") {
                e.preventDefault()
                el.scrollBy({ top: -40, behavior: "smooth" })
                return
              }
              if (e.key === "PageDown") {
                e.preventDefault()
                el.scrollBy({ top: Math.round(el.clientHeight * 0.9), behavior: "smooth" })
                return
              }
              if (e.key === "PageUp") {
                e.preventDefault()
                el.scrollBy({ top: -Math.round(el.clientHeight * 0.9), behavior: "smooth" })
                return
              }
              if (e.key === "Home") {
                e.preventDefault()
                el.scrollTo({ top: 0, behavior: "smooth" })
                return
              }
              if (e.key === "End") {
                e.preventDefault()
                el.scrollTo({ top: el.scrollHeight, behavior: "smooth" })
              }
            }}
          >
            <DataTablePro
              stickyHeader
              columns={[
                {
                  key: "date",
                  header: "Date",
                  sortable: true,
                  className: "w-[130px]",
                  cell: (row) => format(new Date(row.date), "dd MMM yyyy"),
                },
                { key: "refType", header: "Ref", sortable: true, className: "w-[140px]" },
                {
                  key: "description",
                  header: "Description",
                  cell: (row) => (
                    <div className="break-words whitespace-normal">{row.description || ""}</div>
                  ),
                },
                {
                  key: "amount",
                  header: "Amount",
                  sortable: true,
                  className: "w-[140px] text-right",
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
          </div>

          <DialogFooter className="p-4 border-t border-border">
            <DialogClose asChild>
              <Button variant="outline">Close</Button>
            </DialogClose>
          </DialogFooter>
        </DialogContent>
      </Dialog>
     </div>
   )
 }
 
