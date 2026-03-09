"use client"

import { useEffect, useState } from "react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import Link from "next/link"
import { getPurchaseReturns, PurchaseReturn } from "@/lib/purchase-returns"
import { Plus } from "lucide-react"
import { format } from "date-fns"

export default function PurchaseReturnsPage() {
  const [data, setData] = useState<PurchaseReturn[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    loadData()
  }, [])

  async function loadData() {
    try {
      setLoading(true)
      const res = await getPurchaseReturns()
      setData(res)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load purchase returns")
    } finally {
      setLoading(false)
    }
  }

  const columns = [
    {
      key: "returnNumber",
      header: "Return #",
    },
    {
      key: "returnDate",
      header: "Date",
      cell: (row: PurchaseReturn) => format(new Date(row.returnDate), "dd MMM yyyy"),
    },
    {
      key: "partyName",
      header: "Vendor",
    },
    {
      key: "warehouseName",
      header: "Warehouse",
    },
    {
      key: "grandTotal",
      header: "Total",
      cell: (row: PurchaseReturn) => `₹${(row.grandTotal || 0).toFixed(2)}`,
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <PageHeader
          title="Purchase Returns"
          description="Manage vendor returns (Debit Notes)"
        />
        <Link href="/purchase/returns/new">
          <Button className="gap-2">
            <Plus className="h-4 w-4" />
            New Return
          </Button>
        </Link>
      </div>

      {error ? (
        <div className="p-4 rounded-lg border border-destructive/50 bg-destructive/10 text-destructive">
          {error}
        </div>
      ) : (
        <DataTablePro
          columns={columns}
          data={data}
          loading={loading}
          searchKey="partyName"
        />
      )}
    </div>
  )
}
