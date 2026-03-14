"use client"

import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import { PageHeader } from "@/components/ui/page-header"
import { Button } from "@/components/ui/button"
import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { AnnualReport, getAnnualReport } from "@/lib/reports"

export default function AnnualReportPage() {
  const currentYear = useMemo(() => new Date().getFullYear(), [])
  const [yearInput, setYearInput] = useState<string>(String(currentYear))
  const [topLimit, setTopLimit] = useState<string>("10")
  const yearRef = useRef(yearInput)
  const topLimitRef = useRef(topLimit)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [report, setReport] = useState<AnnualReport | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const parsedYear = Number(yearRef.current)
      if (!Number.isInteger(parsedYear) || parsedYear < 1900 || parsedYear > 9999) {
        throw new Error("Enter a valid year (1900–9999)")
      }
      const parsedTopLimit = Number(topLimitRef.current)
      const data = await getAnnualReport({
        year: parsedYear,
        topLimit: Number.isFinite(parsedTopLimit) ? parsedTopLimit : undefined,
      })
      setReport(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load report")
      setReport(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const exportCsv = useCallback(() => {
    if (!report) return
    const headers = ["Month", "Revenue", "Discounts", "COGS", "Expenses", "Gross Profit", "Net Profit"]
    const lines = [headers.join(",")]
    for (const m of report.months) {
      const row = [
        String(m.month),
        String(m.revenue || 0),
        String(m.discounts || 0),
        String(m.cogs || 0),
        String(m.expenses || 0),
        String(m.grossProfit || 0),
        String(m.netProfit || 0),
      ].map((v) => `"${String(v).replaceAll('"', '""')}"`)
      lines.push(row.join(","))
    }
    const blob = new Blob([lines.join("\n")], { type: "text/csv;charset=utf-8" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
      a.download = `annual-report-${yearInput}.csv`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  }, [report, yearInput])

  return (
    <div className="space-y-6">
      <PageHeader title="Annual Report" description="Monthly trends and top customers/items for a year." />

      <div className="flex flex-wrap items-center gap-2">
        <input
          className="w-full max-w-[140px] px-3 py-2 rounded-md border border-input bg-background"
          type="number"
          value={yearInput}
          onChange={(e) => {
            setYearInput(e.target.value)
            yearRef.current = e.target.value
          }}
        />
        <input
          className="w-full max-w-[160px] px-3 py-2 rounded-md border border-input bg-background"
          placeholder="Top limit"
          inputMode="numeric"
          value={topLimit}
          onChange={(e) => {
            setTopLimit(e.target.value)
            topLimitRef.current = e.target.value
          }}
        />
        <Button variant="outline" onClick={load} disabled={loading}>
          Refresh
        </Button>
        <Button variant="outline" onClick={exportCsv} disabled={!report || loading}>
          Export CSV
        </Button>
      </div>

      {error ? <div className="text-sm text-destructive">{error}</div> : null}

      <Tabs defaultValue="months" className="space-y-4">
        <TabsList>
          <TabsTrigger value="months">Monthly Trends</TabsTrigger>
          <TabsTrigger value="items">Top Items</TabsTrigger>
          <TabsTrigger value="customers">Top Customers</TabsTrigger>
        </TabsList>

        <TabsContent value="months">
          <DataTablePro
            columns={[
              { key: "month", header: "Month", sortable: true },
              { key: "revenue", header: "Revenue", sortable: true },
              { key: "discounts", header: "Discounts", sortable: true },
              { key: "cogs", header: "COGS", sortable: true },
              { key: "expenses", header: "Expenses", sortable: true },
              { key: "grossProfit", header: "Gross Profit", sortable: true },
              { key: "netProfit", header: "Net Profit", sortable: true },
            ]}
            data={report?.months ?? []}
            loading={loading}
            filters={null}
            actions={null}
            empty={{ title: "No data", description: "Try a different year" }}
          />
        </TabsContent>

        <TabsContent value="items">
          <DataTablePro
            columns={[
              { key: "itemName", header: "Item", sortable: true },
              { key: "itemCode", header: "Code", sortable: true },
              { key: "quantity", header: "Quantity", sortable: true },
              { key: "amount", header: "Amount", sortable: true },
            ]}
            data={report?.topItems ?? []}
            loading={loading}
            filters={null}
            actions={null}
            empty={{ title: "No data", description: "No sales for this period" }}
          />
        </TabsContent>

        <TabsContent value="customers">
          <DataTablePro
            columns={[
              { key: "partyName", header: "Customer", sortable: true },
              { key: "invoiceCount", header: "Invoices", sortable: true },
              { key: "amount", header: "Amount", sortable: true },
            ]}
            data={report?.topCustomers ?? []}
            loading={loading}
            filters={null}
            actions={null}
            empty={{ title: "No data", description: "No sales for this period" }}
          />
        </TabsContent>
      </Tabs>
    </div>
  )
}
