"use client"

import { useEffect, useState } from "react"
import { PageHeader } from "@/components/ui/page-header"
import { DataTablePro } from "@/components/ui-kit/DataTablePro"
import { listTaxSlabs, deleteTaxSlab, TaxSlab } from "@/lib/tax-slabs"
import { TaxSlabDialog } from "./TaxSlabDialog"
import { ConfirmDialog } from "@/components/ui-kit/ConfirmDialog"
import { Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"

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

  function renderActions(row: TaxSlab) {
    return (
      <ConfirmDialog
        title={`Delete ${row.description}?`}
        description="This action cannot be undone."
        confirmText="Delete"
        onConfirm={async () => {
          try {
            await deleteTaxSlab(row.id)
            setData((prev) => prev.filter((item) => item.id !== row.id))
          } catch (err) {
            setError(err instanceof Error ? err.message : "Failed to delete tax slab")
          }
        }}
      >
        <Button variant="ghost" size="icon" className="h-8 w-8 text-destructive">
          <Trash2 className="h-4 w-4" />
        </Button>
      </ConfirmDialog>
    )
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
    {
      key: "actions",
      header: "",
      cell: renderActions,
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <PageHeader
          title="Tax Slabs"
          description="Manage GST tax slabs and rates"
        />
        <TaxSlabDialog onSuccess={loadData} />
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
