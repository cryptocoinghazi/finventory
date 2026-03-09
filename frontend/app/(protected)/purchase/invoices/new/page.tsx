"use client"

import Link from "next/link"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import { createPurchaseInvoice, PurchaseInvoiceInput } from "@/lib/purchase-invoices"
import { listItems, Item } from "@/lib/items"
import { listParties, Party } from "@/lib/parties"
import { listWarehouses, Warehouse } from "@/lib/warehouses"
import { useEffect, useState } from "react"
import { PurchaseInvoiceForm } from "@/components/purchase/PurchaseInvoiceForm"

export default function NewPurchaseInvoicePage() {
  const router = useRouter()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [parties, setParties] = useState<Party[]>([])
  const [warehouses, setWarehouses] = useState<Warehouse[]>([])
  const [items, setItems] = useState<Item[]>([])

  useEffect(() => {
    async function loadMasters() {
      setLoading(true)
      setError(null)
      try {
        const [p, w, i] = await Promise.all([
          listParties(),
          listWarehouses(),
          listItems(),
        ])
        setParties(p)
        setWarehouses(w)
        setItems(i)
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load masters")
      } finally {
        setLoading(false)
      }
    }
    loadMasters()
  }, [])

  async function onSubmit(data: PurchaseInvoiceInput) {
    try {
      const payload: PurchaseInvoiceInput = {
        ...data,
        invoiceNumber: null, // Auto-generated
        lines: data.lines.map((l) => ({
          itemId: l.itemId,
          quantity: Number(l.quantity),
          unitPrice: Number(l.unitPrice),
        })),
      }
      await createPurchaseInvoice(payload)
      router.push("/purchase/invoices")
      router.refresh()
    } catch (err) {
      console.error(err)
      setError(err instanceof Error ? err.message : "Create failed")
    }
  }

  if (loading) {
    return <div className="p-8 text-center">Loading masters...</div>
  }

  if (error) {
    return (
      <div className="p-8 text-center text-destructive">
        Error: {error}
        <div className="mt-4">
          <Button variant="outline" onClick={() => window.location.reload()}>
            Retry
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="New Purchase Invoice"
        description="Record a new purchase invoice"
        actions={
          <Link href="/purchase/invoices">
            <Button variant="outline">Back</Button>
          </Link>
        }
      />
      
      <PurchaseInvoiceForm
        items={items}
        parties={parties}
        warehouses={warehouses}
        onSubmit={onSubmit}
      />
    </div>
  )
}
