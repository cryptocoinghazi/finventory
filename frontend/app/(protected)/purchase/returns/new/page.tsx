
"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { PageHeader } from "@/components/ui/page-header"
import { Button } from "@/components/ui/button"
import Link from "next/link"
import { PurchaseReturnForm } from "@/components/purchase/PurchaseReturnForm"
import { Item, listItems } from "@/lib/items"
import { Party, listParties } from "@/lib/parties"
import { Warehouse, listWarehouses } from "@/lib/warehouses"
import { createPurchaseReturn, PurchaseReturnInput } from "@/lib/purchase-returns"
import { PurchaseInvoice, listPurchaseInvoices } from "@/lib/purchase-invoices"

export default function NewPurchaseReturnPage() {
  const router = useRouter()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  
  const [items, setItems] = useState<Item[]>([])
  const [parties, setParties] = useState<Party[]>([])
  const [warehouses, setWarehouses] = useState<Warehouse[]>([])
  const [invoices, setInvoices] = useState<PurchaseInvoice[]>([])

  useEffect(() => {
    async function loadMasters() {
      try {
        const [itemsData, partiesData, warehousesData, invoicesData] = await Promise.all([
          listItems(),
          listParties(),
          listWarehouses(),
          listPurchaseInvoices(),
        ])
        setItems(itemsData)
        setParties(partiesData)
        setWarehouses(warehousesData)
        setInvoices(invoicesData)
      } catch (err) {
        setError("Failed to load master data")
        console.error(err)
      } finally {
        setLoading(false)
      }
    }
    loadMasters()
  }, [])

  async function onSubmit(data: PurchaseReturnInput) {
    try {
      const payload: PurchaseReturnInput = {
        ...data,
        returnNumber: "", // Auto-generated
        lines: data.lines.map((l) => ({
          itemId: l.itemId,
          quantity: Number(l.quantity),
          unitPrice: Number(l.unitPrice),
        })),
      }
      await createPurchaseReturn(payload)
      router.push("/purchase/returns")
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
        title="New Purchase Return"
        description="Create a new vendor return (Debit Note)"
        actions={
          <Link href="/purchase/returns">
            <Button variant="outline">Cancel</Button>
          </Link>
        }
      />

      {error ? (
        <div className="p-4 rounded-lg border border-destructive/50 bg-destructive/10 text-destructive">
          {error}
        </div>
      ) : null}

      <PurchaseReturnForm
        items={items}
        parties={parties}
        warehouses={warehouses}
        invoices={invoices}
        onSubmit={onSubmit}
      />
    </div>
  )
}
