"use client"

import { useCallback, useEffect, useState } from "react"
import { PageHeader } from "@/components/ui/page-header"
import { DateRangeFilter } from "@/components/reports/DateRangeFilter"
import { Button } from "@/components/ui/button"
import { getProfitLossReport, ProfitLossReport } from "@/lib/reports"
import { StatCard } from "@/components/ui-kit/StatCard"

export default function ProfitLossReportPage() {
  const [fromDate, setFromDate] = useState("")
  const [toDate, setToDate] = useState("")
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [report, setReport] = useState<ProfitLossReport | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await getProfitLossReport({
        fromDate: fromDate || undefined,
        toDate: toDate || undefined,
      })
      setReport(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load report")
      setReport(null)
    } finally {
      setLoading(false)
    }
  }, [fromDate, toDate])

  useEffect(() => {
    load()
  }, [load])

  const exportCsv = useCallback(() => {
    if (!report) return
    const headers = ["From Date", "To Date", "Revenue", "Discounts", "COGS", "Expenses", "Gross Profit", "Net Profit"]
    const row = [
      report.fromDate ?? "",
      report.toDate ?? "",
      String(report.revenue || 0),
      String(report.discounts || 0),
      String(report.cogs || 0),
      String(report.expenses || 0),
      String(report.grossProfit || 0),
      String(report.netProfit || 0),
    ].map((v) => `"${String(v).replaceAll('"', '""')}"`)
    const lines = [headers.join(","), row.join(",")]
    const blob = new Blob([lines.join("\n")], { type: "text/csv;charset=utf-8" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = `profit-loss-${toDate || new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  }, [report, toDate])

  const exportPdf = useCallback(() => {
    if (!report) return
    const w = window.open("", "_blank", "width=900,height=700")
    if (!w) return

    const title = "Profit & Loss"
    const subtitle = [
      report.fromDate ? `From: ${report.fromDate}` : "",
      report.toDate ? `To: ${report.toDate}` : "",
    ]
      .filter(Boolean)
      .join("  ")
    const html = `
      <html>
        <head>
          <title>${title}</title>
          <style>
            body { font-family: ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Arial; padding: 24px; }
            h1 { margin: 0 0 6px 0; font-size: 18px; }
            .subtitle { margin: 0 0 16px 0; color: #555; font-size: 12px; }
            table { width: 420px; border-collapse: collapse; font-size: 12px; }
            th, td { border: 1px solid #ddd; padding: 8px; }
            th { background: #f7f7f7; text-align: left; }
            td:last-child { text-align: right; }
          </style>
        </head>
        <body>
          <h1>${title}</h1>
          <div class="subtitle">${subtitle}</div>
          <table>
            <tbody>
              <tr><th>Revenue</th><td>${report.revenue ?? 0}</td></tr>
              <tr><th>Discounts</th><td>${report.discounts ?? 0}</td></tr>
              <tr><th>COGS</th><td>${report.cogs ?? 0}</td></tr>
              <tr><th>Gross Profit</th><td>${report.grossProfit ?? 0}</td></tr>
              <tr><th>Expenses</th><td>${report.expenses ?? 0}</td></tr>
              <tr><th>Net Profit</th><td>${report.netProfit ?? 0}</td></tr>
            </tbody>
          </table>
        </body>
      </html>
    `
    w.document.open()
    w.document.write(html)
    w.document.close()
    w.focus()
    w.print()
  }, [report])

  return (
    <div className="space-y-6">
      <PageHeader title="Profit & Loss" description="Gross and net profit over a date range." />

      <div className="flex flex-wrap items-center gap-2">
        <DateRangeFilter
          fromDate={fromDate}
          toDate={toDate}
          onChange={({ fromDate: f, toDate: t }) => {
            setFromDate(f)
            setToDate(t)
          }}
        />
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

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <StatCard title="Revenue" value={report?.revenue ?? 0} />
        <StatCard title="Discounts" value={report?.discounts ?? 0} />
        <StatCard title="COGS" value={report?.cogs ?? 0} />
        <StatCard title="Gross Profit" value={report?.grossProfit ?? 0} />
        <StatCard title="Expenses" value={report?.expenses ?? 0} />
        <StatCard title="Net Profit" value={report?.netProfit ?? 0} />
      </div>
    </div>
  )
}

