"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { PageHeader } from "@/components/ui/page-header"
import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import { DateRangeFilter } from "@/components/reports/DateRangeFilter"
import { Button } from "@/components/ui/button"
import {
  getSalesReport,
  InvoicePaymentStatus,
  ReportInvoiceStatus,
  SalesGroupBy,
  SalesReportResponse,
} from "@/lib/reports"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { SmartSelect } from "@/components/ui-kit/SmartSelect"
import { Item, listItems } from "@/lib/items"
import { Party, listParties } from "@/lib/parties"
import { StatCard } from "@/components/ui-kit/StatCard"

export default function SalesReportPage() {
  const [fromDate, setFromDate] = useState("")
  const [toDate, setToDate] = useState("")
  const [groupBy, setGroupBy] = useState<SalesGroupBy>("DAY")
  const [paymentStatus, setPaymentStatus] = useState<"__ALL__" | InvoicePaymentStatus>("__ALL__")
  const [invoiceStatus, setInvoiceStatus] = useState<ReportInvoiceStatus>("ACTIVE")
  const [category, setCategory] = useState("")
  const [itemId, setItemId] = useState("")
  const [partyId, setPartyId] = useState("")

  const [items, setItems] = useState<Item[]>([])
  const [customers, setCustomers] = useState<Party[]>([])

  const [report, setReport] = useState<SalesReportResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    const loadLookups = async () => {
      try {
        const [it, cust] = await Promise.all([listItems(), listParties("CUSTOMER")])
        if (!active) return
        setItems(it)
        setCustomers(cust)
      } catch {
        if (!active) return
        setItems([])
        setCustomers([])
      }
    }
    loadLookups()
    return () => {
      active = false
    }
  }, [])

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await getSalesReport({
        fromDate: fromDate || undefined,
        toDate: toDate || undefined,
        groupBy,
        itemId: itemId || undefined,
        category: category.trim() || undefined,
        partyId: partyId || undefined,
        paymentStatus: paymentStatus === "__ALL__" ? undefined : paymentStatus,
        invoiceStatus,
      })
      setReport(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load report")
      setReport(null)
    } finally {
      setLoading(false)
    }
  }, [category, fromDate, groupBy, invoiceStatus, itemId, partyId, paymentStatus, toDate])

  useEffect(() => {
    load()
  }, [load])

  const totals = report?.totals
  const buckets = report?.buckets ?? []

  const exportCsv = useCallback(() => {
    if (!report) return
    const headers = [
      "Period Start",
      "Period End",
      "Label",
      "Invoices",
      "Total Amount",
      "Total Discount",
      "Total Paid",
      "Total Pending",
    ]
    const lines = [headers.join(",")]
    for (const b of report.buckets) {
      const row = [
        b.periodStart || "",
        b.periodEnd || "",
        b.label || "",
        String(b.invoiceCount || 0),
        String(b.totalAmount || 0),
        String(b.totalDiscount || 0),
        String(b.totalPaid || 0),
        String(b.totalPending || 0),
      ].map((v) => `"${String(v).replaceAll('"', '""')}"`)
      lines.push(row.join(","))
    }

    const blob = new Blob([lines.join("\n")], { type: "text/csv;charset=utf-8" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = `sales-report-${toDate || new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  }, [report, toDate])

  const exportPdf = useCallback(() => {
    if (!report) return

    const w = window.open("", "_blank", "width=1024,height=768")
    if (!w) return

    const tableRows = report.buckets
      .map(
        (b) => `
        <tr>
          <td>${b.periodStart ?? ""}</td>
          <td>${b.periodEnd ?? ""}</td>
          <td>${b.label ?? ""}</td>
          <td style="text-align:right">${b.invoiceCount ?? 0}</td>
          <td style="text-align:right">${b.totalAmount ?? 0}</td>
          <td style="text-align:right">${b.totalDiscount ?? 0}</td>
          <td style="text-align:right">${b.totalPaid ?? 0}</td>
          <td style="text-align:right">${b.totalPending ?? 0}</td>
        </tr>
      `
      )
      .join("")

    const title = "Sales Report"
    const subtitle = [fromDate ? `From: ${fromDate}` : "", toDate ? `To: ${toDate}` : ""].filter(Boolean).join("  ")
    const html = `
      <html>
        <head>
          <title>${title}</title>
          <style>
            body { font-family: ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Arial; padding: 24px; }
            h1 { margin: 0 0 6px 0; font-size: 18px; }
            .subtitle { margin: 0 0 16px 0; color: #555; font-size: 12px; }
            table { width: 100%; border-collapse: collapse; font-size: 12px; }
            th, td { border: 1px solid #ddd; padding: 8px; }
            th { background: #f7f7f7; text-align: left; }
            .totals { margin-top: 16px; font-size: 12px; }
            .totals td { border: none; padding: 4px 0; }
          </style>
        </head>
        <body>
          <h1>${title}</h1>
          <div class="subtitle">${subtitle}</div>
          <table>
            <thead>
              <tr>
                <th>Period Start</th>
                <th>Period End</th>
                <th>Label</th>
                <th style="text-align:right">Invoices</th>
                <th style="text-align:right">Total Amount</th>
                <th style="text-align:right">Total Discount</th>
                <th style="text-align:right">Total Paid</th>
                <th style="text-align:right">Total Pending</th>
              </tr>
            </thead>
            <tbody>
              ${tableRows}
            </tbody>
          </table>
          ${
            totals
              ? `
                <table class="totals">
                  <tr><td><strong>Total Invoices</strong></td><td style="text-align:right">${totals.invoiceCount}</td></tr>
                  <tr><td><strong>Total Amount</strong></td><td style="text-align:right">${totals.totalAmount}</td></tr>
                  <tr><td><strong>Total Discount</strong></td><td style="text-align:right">${totals.totalDiscount}</td></tr>
                  <tr><td><strong>Total Paid</strong></td><td style="text-align:right">${totals.totalPaid}</td></tr>
                  <tr><td><strong>Total Pending</strong></td><td style="text-align:right">${totals.totalPending}</td></tr>
                </table>
              `
              : ""
          }
        </body>
      </html>
    `
    w.document.open()
    w.document.write(html)
    w.document.close()
    w.focus()
    w.print()
  }, [fromDate, report, toDate, totals])

  const itemOptions = useMemo(() => [{ id: "__all__", name: "All Items", code: "" } as Item, ...items], [items])
  const customerOptions = useMemo(
    () => [{ id: "__all__", name: "All Customers", type: "CUSTOMER" } as Party, ...customers],
    [customers]
  )

  return (
    <div className="space-y-6">
      <PageHeader title="Sales Report" description="Sales totals grouped by day/week/month or full range." />

      <div className="flex flex-wrap items-center gap-2">
        <DateRangeFilter
          fromDate={fromDate}
          toDate={toDate}
          onChange={({ fromDate: f, toDate: t }) => {
            setFromDate(f)
            setToDate(t)
          }}
        />
        <div className="w-full max-w-[170px]">
          <Select value={groupBy} onValueChange={(v) => setGroupBy(v as SalesGroupBy)}>
            <SelectTrigger>
              <SelectValue placeholder="Group by" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="DAY">Day</SelectItem>
              <SelectItem value="WEEK">Week</SelectItem>
              <SelectItem value="MONTH">Month</SelectItem>
              <SelectItem value="RANGE">Range</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div className="w-full max-w-[190px]">
          <Select value={invoiceStatus} onValueChange={(v) => setInvoiceStatus(v as ReportInvoiceStatus)}>
            <SelectTrigger>
              <SelectValue placeholder="Invoice status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ACTIVE">Active</SelectItem>
              <SelectItem value="CANCELLED">Cancelled</SelectItem>
              <SelectItem value="DELETED">Deleted</SelectItem>
              <SelectItem value="ALL">All</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div className="w-full max-w-[190px]">
          <Select value={paymentStatus} onValueChange={(v) => setPaymentStatus(v as "__ALL__" | InvoicePaymentStatus)}>
            <SelectTrigger>
              <SelectValue placeholder="Payment status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="__ALL__">All Payments</SelectItem>
              <SelectItem value="PENDING">Pending</SelectItem>
              <SelectItem value="PARTIAL">Partial</SelectItem>
              <SelectItem value="PAID">Paid</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <input
          className="w-full max-w-[220px] px-3 py-2 rounded-md border border-input bg-background"
          placeholder="Category"
          value={category}
          onChange={(e) => setCategory(e.target.value)}
        />
        <div className="w-full max-w-[260px]">
          <SmartSelect<Item>
            value={itemId}
            onSelect={(next) => setItemId(next === "__all__" ? "" : next)}
            placeholder="All Items"
            searchPlaceholder="Search item..."
            options={itemOptions}
            labelKey="name"
            valueKey="id"
            filterOption={(it, q) => (it.name + " " + it.code).toLowerCase().includes(q)}
          />
        </div>
        <div className="w-full max-w-[260px]">
          <SmartSelect<Party>
            value={partyId}
            onSelect={(next) => setPartyId(next === "__all__" ? "" : next)}
            placeholder="All Customers"
            searchPlaceholder="Search customer..."
            options={customerOptions}
            labelKey="name"
            valueKey="id"
            filterOption={(p, q) => p.name.toLowerCase().includes(q)}
          />
        </div>
        <Button variant="outline" onClick={load} disabled={loading}>
          Refresh
        </Button>
        <Button variant="outline" onClick={exportCsv} disabled={!report || loading}>
          Export CSV
        </Button>
        <Button variant="outline" onClick={exportPdf} disabled={!report || loading}>
          Export PDF
        </Button>
      </div>

      {error ? <div className="text-sm text-destructive">{error}</div> : null}

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        <StatCard title="Invoices" value={totals?.invoiceCount ?? 0} />
        <StatCard title="Total Amount" value={totals?.totalAmount ?? 0} />
        <StatCard title="Discounts" value={totals?.totalDiscount ?? 0} />
        <StatCard title="Paid" value={totals?.totalPaid ?? 0} />
        <StatCard title="Pending" value={totals?.totalPending ?? 0} />
      </div>

      <DataTablePro
        columns={[
          { key: "label", header: "Period", sortable: true, cell: (r) => r.label || "-" },
          { key: "periodStart", header: "Start", sortable: true, cell: (r) => r.periodStart || "-" },
          { key: "periodEnd", header: "End", sortable: true, cell: (r) => r.periodEnd || "-" },
          { key: "invoiceCount", header: "Invoices", sortable: true },
          { key: "totalAmount", header: "Total", sortable: true },
          { key: "totalDiscount", header: "Discount", sortable: true },
          { key: "totalPaid", header: "Paid", sortable: true },
          { key: "totalPending", header: "Pending", sortable: true },
        ]}
        data={buckets}
        loading={loading}
        filters={null}
        actions={null}
        empty={{ title: "No data", description: "Try adjusting filters" }}
      />
    </div>
  )
}
