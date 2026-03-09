"use client"

import { useEffect, useState } from "react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import { listTaxSlabs, TaxSlab } from "@/lib/tax-slabs"
import { Plus } from "lucide-react"

export default function TaxSlabsPage() {
  const [data, setData] = useState<TaxSlab[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    loadData()
  }, [])

  async function loadData() {
    try {
      setLoading(true)
      const res = await listTaxSlabs()
      setData(res)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load tax slabs")
    } finally {
      setLoading(false)
    }
  }

  const columns = [
    {
      key: "rate",
      header: "Rate (%)",
    },
    {
      key: "description",
      header: "Description",
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <PageHeader
          title="Tax Slabs"
          description="Manage GST tax slabs and rates"
        />
        <Button className="gap-2">
          <Plus className="h-4 w-4" />
          New Tax Slab
        </Button>
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
          searchKey="description"
        />
      )}
    </div>
  )
}
