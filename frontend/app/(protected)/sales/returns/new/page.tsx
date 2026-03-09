"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { PageHeader } from "@/components/ui/page-header"
import { Button } from "@/components/ui/button"
import Link from "next/link"
import { SalesReturnForm } from "@/components/sales/SalesReturnForm"
import { Item, listItems } from "@/lib/items"
import { Party, listParties } from "@/lib/parties"
import { Warehouse, listWarehouses } from "@/lib/warehouses"
import { createSalesReturn, SalesReturnInput } from "@/lib/sales-returns"

export default function NewSalesReturnPage() {
  const router = useRouter()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  
  const [items, setItems] = useState<Item[]>([])
  const [parties, setParties] = useState<Party[]>([])
  const [warehouses, setWarehouses] = useState<Warehouse[]>([])

  useEffect(() => {
    async function loadMasters() {
      try {
        const [itemsData, partiesData, warehousesData] = await Promise.all([
          listItems(),
          listParties(),
          listWarehouses(),
        ])
        setItems(itemsData)
        setParties(partiesData)
        setWarehouses(warehousesData)
      } catch (err) {
        setError("Failed to load master data")
        console.error(err)
      } finally {
        setLoading(false)
      }
    }
    loadMasters()
  }, [])

  async function onSubmit(data: SalesReturnInput) {
    try {
      const payload: SalesReturnInput = {
        ...data,
        returnNumber: "", // Auto-generated
        lines: data.lines.map((l) => ({
          itemId: l.itemId,
          quantity: Number(l.quantity),
          unitPrice: Number(l.unitPrice),
        })),
      }
      await createSalesReturn(payload)
      router.push("/sales/returns")
      router.refresh()
    } catch (err) {
      console.error(err)
      setError(err instanceof Error ? err.message : "Create failed")
    }
  }

  if (loading) return <div>Loading...</div>

  return (
    <div className="space-y-6">
      <PageHeader
        title="New Sales Return"
        description="Create a new customer return (Credit Note)"
        actions={
          <Link href="/sales/returns">
            <Button variant="outline">Cancel</Button>
          </Link>
        }
      />

      {error ? (
        <div className="p-4 rounded-lg border border-destructive/50 bg-destructive/10 text-destructive">
          {error}
        </div>
      ) : null}

      <SalesReturnForm
        items={items}
        parties={parties}
        warehouses={warehouses}
        onSubmit={onSubmit}
      />
    </div>
  )
}
