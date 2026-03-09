"use client"

import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import { WarehouseForm } from "@/components/masters/WarehouseForm"
import { PageHeader } from "@/components/ui/page-header"
import { Button } from "@/components/ui/button"
import { getWarehouse, updateWarehouse, Warehouse, WarehouseInput } from "@/lib/warehouses"
import Link from "next/link"

export default function EditWarehousePage() {
  const params = useParams()
  const router = useRouter()
  const id = params.id as string
  
  const [warehouse, setWarehouse] = useState<Warehouse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    async function load() {
      try {
        setLoading(true)
        const data = await getWarehouse(id)
        setWarehouse(data)
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load warehouse")
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id])

  async function onSubmit(input: WarehouseInput) {
    await updateWarehouse(id, input)
    router.push("/masters/warehouses")
    router.refresh()
  }

  if (loading) {
    return <div className="p-8 text-center">Loading...</div>
  }

  if (error || !warehouse) {
    return (
      <div className="p-8 text-center text-destructive">
        Error: {error || "Warehouse not found"}
        <div className="mt-4">
          <Link href="/masters/warehouses">
            <Button variant="outline">Back to List</Button>
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Edit Warehouse"
        description={`Edit details for ${warehouse.name}`}
        actions={
          <Link href="/masters/warehouses">
            <Button variant="outline">Back</Button>
          </Link>
        }
      />
      <WarehouseForm 
        submitLabel="Update Warehouse" 
        initialValue={warehouse}
        onSubmit={onSubmit} 
      />
    </div>
  )
}
