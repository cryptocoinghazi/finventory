"use client"

import Link from "next/link"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import { createPurchaseInvoice, PurchaseInvoiceInput } from "@/lib/purchase-invoices"
import { listItems, Item } from "@/lib/items"
import { listParties, Party } from "@/lib/parties"
import { listWarehouses, Warehouse } from "@/lib/warehouses"
import { useEffect, useMemo, useState } from "react"

export default function NewPurchaseInvoicePage() {
  const router = useRouter()
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [parties, setParties] = useState<Party[]>([])
  const [warehouses, setWarehouses] = useState<Warehouse[]>([])
  const [items, setItems] = useState<Item[]>([])

  const [invoiceDate, setInvoiceDate] = useState(new Date().toISOString().slice(0, 10))
  const [vendorInvoiceNumber, setVendorInvoiceNumber] = useState("")
  const [partyId, setPartyId] = useState("")
  const [warehouseId, setWarehouseId] = useState("")
  const [itemId, setItemId] = useState("")
  const [quantity, setQuantity] = useState(1)
  const [unitPrice, setUnitPrice] = useState(1)

  const vendors = useMemo(() => parties.filter((p) => p.type === "VENDOR"), [parties])

  useEffect(() => {
    async function loadMasters() {
      setLoading(true)
      setError(null)
      try {
        const [p, w, i] = await Promise.all([listParties(), listWarehouses(), listItems()])
        setParties(p)
        setWarehouses(w)
        setItems(i)

        const defaultVendorId = p.find((x) => x.type === "VENDOR")?.id ?? ""
        const defaultWarehouseId = w[0]?.id ?? ""
        const defaultItem = i[0]

        setPartyId(defaultVendorId)
        setWarehouseId(defaultWarehouseId)
        setItemId(defaultItem?.id ?? "")
        setUnitPrice(defaultItem?.unitPrice ?? 1)
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load masters")
      } finally {
        setLoading(false)
      }
    }
    loadMasters()
  }, [])

  useEffect(() => {
    const selected = items.find((x) => x.id === itemId)
    if (selected) {
      setUnitPrice(selected.unitPrice)
    }
  }, [itemId, items])

  async function onCreateDemo() {
    setError(null)
    setSaving(true)
    try {
      if (!partyId || !warehouseId || !itemId) {
        throw new Error("Select vendor, warehouse, and item")
      }

      const input: PurchaseInvoiceInput = {
        invoiceDate,
        partyId,
        warehouseId,
        invoiceNumber: null,
        vendorInvoiceNumber: vendorInvoiceNumber.trim() ? vendorInvoiceNumber.trim() : null,
        lines: [
          {
            itemId,
            quantity,
            unitPrice,
          },
        ],
      }
      await createPurchaseInvoice(input)
      router.push("/purchase/invoices")
      router.refresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Create failed")
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="New Purchase Invoice"
        description="Create a purchase invoice. Coming Soon: full line editor."
        actions={
          <Link href="/purchase/invoices">
            <Button variant="outline">Back</Button>
          </Link>
        }
      />
      {error ? <div className="text-sm text-destructive">{error}</div> : null}
      <div className="rounded-2xl border border-border p-4">
        <div className="text-sm text-muted-foreground">
          The full invoice form is coming soon. This creates a simple 1-line invoice using
          existing masters.
        </div>

        {loading ? (
          <div className="mt-3 text-sm text-muted-foreground">Loading...</div>
        ) : vendors.length === 0 || warehouses.length === 0 || items.length === 0 ? (
          <div className="mt-3 text-sm text-muted-foreground">
            Missing masters. Create at least one vendor, warehouse, and item in Masters first.
          </div>
        ) : (
          <div className="mt-4 grid gap-3 md:grid-cols-2">
            <label className="grid gap-1 text-sm">
              <span className="text-muted-foreground">Invoice Date</span>
              <input
                className="px-3 py-2 rounded-md border border-input bg-background"
                type="date"
                value={invoiceDate}
                onChange={(e) => setInvoiceDate(e.target.value)}
              />
            </label>

            <label className="grid gap-1 text-sm">
              <span className="text-muted-foreground">Vendor Invoice Number</span>
              <input
                className="px-3 py-2 rounded-md border border-input bg-background"
                value={vendorInvoiceNumber}
                onChange={(e) => setVendorInvoiceNumber(e.target.value)}
                placeholder="Optional"
              />
            </label>

            <label className="grid gap-1 text-sm">
              <span className="text-muted-foreground">Vendor</span>
              <select
                className="px-3 py-2 rounded-md border border-input bg-background"
                value={partyId}
                onChange={(e) => setPartyId(e.target.value)}
              >
                {vendors.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </label>

            <label className="grid gap-1 text-sm">
              <span className="text-muted-foreground">Warehouse</span>
              <select
                className="px-3 py-2 rounded-md border border-input bg-background"
                value={warehouseId}
                onChange={(e) => setWarehouseId(e.target.value)}
              >
                {warehouses.map((w) => (
                  <option key={w.id} value={w.id}>
                    {w.name}
                  </option>
                ))}
              </select>
            </label>

            <label className="grid gap-1 text-sm">
              <span className="text-muted-foreground">Item</span>
              <select
                className="px-3 py-2 rounded-md border border-input bg-background"
                value={itemId}
                onChange={(e) => setItemId(e.target.value)}
              >
                {items.map((it) => (
                  <option key={it.id} value={it.id}>
                    {it.code} - {it.name}
                  </option>
                ))}
              </select>
            </label>

            <label className="grid gap-1 text-sm">
              <span className="text-muted-foreground">Quantity</span>
              <input
                className="px-3 py-2 rounded-md border border-input bg-background"
                type="number"
                min={0.01}
                step={0.01}
                value={quantity}
                onChange={(e) => setQuantity(Number(e.target.value))}
              />
            </label>

            <label className="grid gap-1 text-sm">
              <span className="text-muted-foreground">Unit Price</span>
              <input
                className="px-3 py-2 rounded-md border border-input bg-background"
                type="number"
                min={0}
                step={0.01}
                value={unitPrice}
                onChange={(e) => setUnitPrice(Number(e.target.value))}
              />
            </label>

            <div className="md:col-span-2 mt-2 flex items-center gap-2">
              <Button onClick={onCreateDemo} disabled={saving}>
                {saving ? "Creating..." : "Create Invoice"}
              </Button>
              <Link href="/purchase/invoices">
                <Button variant="outline" type="button">
                  Cancel
                </Button>
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
