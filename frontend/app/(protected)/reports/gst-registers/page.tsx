"use client"

import { useEffect, useState } from "react"
import { PageHeader } from "@/components/ui/page-header"
import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { getGstr1, getGstr2, getGstr3b, GstRegisterEntry, Gstr3b } from "@/lib/reports"
import { format } from "date-fns"

export default function GstRegistersPage() {
  const [gstr1Data, setGstr1Data] = useState<GstRegisterEntry[]>([])
  const [gstr2Data, setGstr2Data] = useState<GstRegisterEntry[]>([])
  const [gstr3bData, setGstr3bData] = useState<Gstr3b | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    loadData()
  }, [])

  async function loadData() {
    try {
      setLoading(true)
      const [gstr1, gstr2, gstr3b] = await Promise.all([
        getGstr1(),
        getGstr2(),
        getGstr3b(),
      ])
      setGstr1Data(gstr1)
      setGstr2Data(gstr2)
      setGstr3bData(gstr3b)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load GST reports")
    } finally {
      setLoading(false)
    }
  }

  const columns = [
    {
      key: "invoiceNumber",
      header: "Invoice #",
    },
    {
      key: "invoiceDate",
      header: "Date",
      cell: (row: GstRegisterEntry) => format(new Date(row.invoiceDate), "dd MMM yyyy"),
    },
    {
      key: "partyName",
      header: "Party Name",
    },
    {
      key: "partyGstin",
      header: "GSTIN",
    },
    {
      key: "taxableValue",
      header: "Taxable Value",
      cell: (row: GstRegisterEntry) => `₹${(row.taxableValue || 0).toFixed(2)}`,
    },
    {
      key: "totalAmount", // Assuming totalAmount is what we want to display as Total
      header: "Total Amount",
      cell: (row: GstRegisterEntry) => `₹${(row.totalAmount || 0).toFixed(2)}`,
    },
  ]

  return (
    <div className="space-y-6">
      <PageHeader
        title="GST Registers"
        description="View GSTR-1, GSTR-2 and GSTR-3B reports"
      />

      {error && (
        <div className="p-4 rounded-lg border border-destructive/50 bg-destructive/10 text-destructive">
          {error}
        </div>
      )}

      <Tabs defaultValue="gstr1" className="w-full">
        <TabsList>
          <TabsTrigger value="gstr1">GSTR-1 (Sales)</TabsTrigger>
          <TabsTrigger value="gstr2">GSTR-2 (Purchases)</TabsTrigger>
          <TabsTrigger value="gstr3b">GSTR-3B (Summary)</TabsTrigger>
        </TabsList>

        <TabsContent value="gstr1" className="space-y-4">
          <DataTablePro
            columns={columns}
            data={gstr1Data}
            loading={loading}
            searchKey="partyName"
          />
        </TabsContent>

        <TabsContent value="gstr2" className="space-y-4">
          <DataTablePro
            columns={columns}
            data={gstr2Data}
            loading={loading}
            searchKey="partyName"
          />
        </TabsContent>

        <TabsContent value="gstr3b" className="space-y-4">
          {gstr3bData ? (
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Outward Taxable</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">₹{(gstr3bData.outwardTaxableValue || 0).toFixed(2)}</div>
                </CardContent>
              </Card>
              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Outward Tax (IGST+CGST+SGST)</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">
                    ₹{((gstr3bData.outwardIgst || 0) + (gstr3bData.outwardCgst || 0) + (gstr3bData.outwardSgst || 0)).toFixed(2)}
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">ITC Available</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">
                    ₹{((gstr3bData.itcIgst || 0) + (gstr3bData.itcCgst || 0) + (gstr3bData.itcSgst || 0)).toFixed(2)}
                  </div>
                </CardContent>
              </Card>
              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">Net Tax Payable</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">₹{(gstr3bData.netTaxPayable || 0).toFixed(2)}</div>
                </CardContent>
              </Card>
            </div>
          ) : (
            <div>No Data Available</div>
          )}
        </TabsContent>
      </Tabs>
    </div>
  )
}
