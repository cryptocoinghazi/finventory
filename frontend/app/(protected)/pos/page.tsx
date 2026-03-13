"use client"

import Link from "next/link"
import { useRouter } from "next/navigation"
import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import { Button } from "@/components/ui/button"
import { PageHeader } from "@/components/ui/page-header"
import { Input } from "@/components/ui/input"
import { Card, CardContent } from "@/components/ui/card"
import { SmartSelect } from "@/components/ui-kit/SmartSelect"
import { Trash2 } from "lucide-react"
import { createItem, listItems, Item } from "@/lib/items"
import { createParty, listParties, Party } from "@/lib/parties"
import { listWarehouses, Warehouse } from "@/lib/warehouses"
import { createSalesInvoice, SalesInvoiceInput, SalesInvoice } from "@/lib/sales-invoices"
import { OfferValidationResponse, validateOffer } from "@/lib/offers"
import { getOrganizationProfile, OrganizationProfile } from "@/lib/settings"
import { getCurrentUser } from "@/lib/users"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"

type PosLine = {
  itemId: string
  quantity: number
  unitPrice: number
}

export default function PosPage() {
  const router = useRouter()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [items, setItems] = useState<Item[]>([])
  const [parties, setParties] = useState<Party[]>([])
  const [warehouses, setWarehouses] = useState<Warehouse[]>([])
  const [organization, setOrganization] = useState<OrganizationProfile | null>(null)

  const [invoiceDate, setInvoiceDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [partyId, setPartyId] = useState("")
  const [warehouseId, setWarehouseId] = useState("")
  const [receiptDateTime, setReceiptDateTime] = useState(() => new Date().toISOString())
  const [lines, setLines] = useState<PosLine[]>([])
  const [created, setCreated] = useState<SalesInvoice | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [discountMode, setDiscountMode] = useState<"PERCENT" | "FLAT">("PERCENT")
  const [discountValue, setDiscountValue] = useState(0)
  const [couponCode, setCouponCode] = useState("")
  const [couponError, setCouponError] = useState<string | null>(null)
  const [couponLoading, setCouponLoading] = useState(false)
  const [appliedOffer, setAppliedOffer] = useState<OfferValidationResponse | null>(null)

  const [createCustomerOpen, setCreateCustomerOpen] = useState(false)
  const [newCustomerName, setNewCustomerName] = useState("")
  const [newCustomerPhone, setNewCustomerPhone] = useState("")
  const [creatingCustomer, setCreatingCustomer] = useState(false)
  const [canCreateItems, setCanCreateItems] = useState<boolean | null>(null)
  const [createItemOpen, setCreateItemOpen] = useState(false)
  const [newItemName, setNewItemName] = useState("")
  const [newItemCode, setNewItemCode] = useState("")
  const [newItemBarcode, setNewItemBarcode] = useState("")
  const [newItemUnitPrice, setNewItemUnitPrice] = useState<number>(0)
  const [newItemTaxRate, setNewItemTaxRate] = useState<number>(0)
  const [newItemUom, setNewItemUom] = useState("PCS")
  const [newItemCodeEdited, setNewItemCodeEdited] = useState(false)
  const [creatingItem, setCreatingItem] = useState(false)
  const [createItemError, setCreateItemError] = useState<string | null>(null)
  const [barcodeValue, setBarcodeValue] = useState("")
  const [barcodeError, setBarcodeError] = useState<string | null>(null)
  const barcodeRef = useRef<HTMLInputElement>(null)
  const barcodeValueRef = useRef("")

  useEffect(() => {
    async function loadMasters() {
      setLoading(true)
      setError(null)
      try {
        const [p, w, i, user] = await Promise.all([
          listParties(),
          listWarehouses(),
          listItems(),
          getCurrentUser().catch(() => null),
        ])
        setParties(p)
        setWarehouses(w)
        setItems(i)
        setCanCreateItems(user ? user.role === "ADMIN" : null)

        const defaultCustomer =
          p.find((x) => x.type === "CUSTOMER" && /walk/i.test(x.name)) ??
          p.find((x) => x.type === "CUSTOMER") ??
          null
        const defaultWarehouse = w[0] ?? null

        setPartyId((prev) => prev || defaultCustomer?.id || "")
        setWarehouseId((prev) => prev || defaultWarehouse?.id || "")

        try {
          const org = await getOrganizationProfile()
          setOrganization(org)
        } catch {
          setOrganization(null)
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load masters")
      } finally {
        setLoading(false)
      }
    }
    loadMasters()
  }, [])

  useEffect(() => {
    document.body.classList.add("pos-print-scope")
    return () => {
      document.body.classList.remove("pos-print-scope")
    }
  }, [])

  useEffect(() => {
    requestAnimationFrame(() => {
      if (!loading && !createCustomerOpen) {
        barcodeRef.current?.focus()
      }
    })
  }, [createCustomerOpen, loading])

  const customers = useMemo(() => parties.filter((p) => p.type === "CUSTOMER"), [parties])

  const itemById = useMemo(() => {
    return new Map(items.map((i) => [i.id, i]))
  }, [items])

  const itemIdByBarcode = useMemo(() => {
    const map = new Map<string, string>()
    for (const it of items) {
      const b = (it.barcode ?? "").trim()
      if (!b) continue
      if (!map.has(b)) {
        map.set(b, it.id)
      }
    }
    return map
  }, [items])

  const partyById = useMemo(() => {
    return new Map(parties.map((p) => [p.id, p]))
  }, [parties])

  const preview = useMemo(() => {
    const subtotal = lines.reduce((acc, l) => acc + Number(l.quantity) * Number(l.unitPrice), 0)
    const couponDiscountAmount = appliedOffer ? Number(appliedOffer.discountAmount) : 0
    const rawDiscount =
      discountValue > 0
        ? discountMode === "PERCENT"
          ? (subtotal * discountValue) / 100
          : discountValue
        : 0
    const discountAmount = Math.max(0, Math.min(subtotal, rawDiscount))
    const grandTotal = Math.max(0, subtotal - discountAmount - couponDiscountAmount)
    return { subtotal, discountAmount, couponDiscountAmount, grandTotal }
  }, [appliedOffer, discountMode, discountValue, lines])

  const receiptDateTimeLabel = useMemo(() => {
    const d = new Date(receiptDateTime)
    if (Number.isNaN(d.getTime())) return receiptDateTime
    return d.toLocaleString("en-IN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    })
  }, [receiptDateTime])

  function resetPos() {
    setLines([])
    setCreated(null)
    setError(null)
    setSubmitting(false)
    setInvoiceDate(new Date().toISOString().slice(0, 10))
    setReceiptDateTime(new Date().toISOString())
    setDiscountMode("PERCENT")
    setDiscountValue(0)
    setCouponCode("")
    setCouponError(null)
    setCouponLoading(false)
    setAppliedOffer(null)
  }

  async function applyCoupon(codeRaw: string) {
    const code = codeRaw.trim()
    if (!code) {
      setCouponError("Enter a coupon code")
      setAppliedOffer(null)
      return
    }

    setCouponLoading(true)
    setCouponError(null)
    try {
      if (discountValue !== 0) {
        setDiscountValue(0)
      }
      const taxableSubtotal = lines.reduce((acc, l) => acc + Number(l.quantity) * Number(l.unitPrice), 0)
      const result = await validateOffer({
        code,
        asOfDate: invoiceDate,
        taxableSubtotal,
        lines: lines.map((l) => ({
          itemId: l.itemId,
          quantity: Number(l.quantity),
          unitPrice: Number(l.unitPrice),
        })),
      })
      setAppliedOffer(result)
      setCouponCode(result.code)
    } catch (err) {
      setAppliedOffer(null)
      setCouponError(err instanceof Error ? err.message : "Failed to apply coupon")
    } finally {
      setCouponLoading(false)
    }
  }

  useEffect(() => {
    if (!appliedOffer) return
    const code = (couponCode || appliedOffer.code || "").trim()
    if (!code) return

    const t = window.setTimeout(async () => {
      try {
        const taxableSubtotal = lines.reduce((acc, l) => acc + Number(l.quantity) * Number(l.unitPrice), 0)
        const result = await validateOffer({
          code,
          asOfDate: invoiceDate,
          taxableSubtotal,
          lines: lines.map((l) => ({
            itemId: l.itemId,
            quantity: Number(l.quantity),
            unitPrice: Number(l.unitPrice),
          })),
        })
        setAppliedOffer(result)
        setCouponError(null)
      } catch (err) {
        setAppliedOffer(null)
        setCouponError(err instanceof Error ? err.message : "Coupon is no longer valid")
      }
    }, 300)

    return () => window.clearTimeout(t)
  }, [appliedOffer, couponCode, invoiceDate, lines])

  const addItem = useCallback((itemId: string) => {
    const item = itemById.get(itemId)
    if (!item) return

    setLines((prev) => {
      const idx = prev.findIndex((l) => l.itemId === itemId && Number(l.unitPrice) === Number(item.unitPrice))
      if (idx >= 0) {
        const next = [...prev]
        next[idx] = { ...next[idx], quantity: Number(next[idx].quantity) + 1 }
        return next
      }
      return [...prev, { itemId, quantity: 1, unitPrice: Number(item.unitPrice) || 0 }]
    })
  }, [itemById])

  const handleBarcodeScanSubmit = useCallback((scannedRaw?: string) => {
    const scanned = (scannedRaw ?? barcodeValueRef.current).trim()
    if (!scanned) return

    const itemId = itemIdByBarcode.get(scanned)
    if (!itemId) {
      setBarcodeError(`Barcode not found: ${scanned}`)
      barcodeValueRef.current = ""
      setBarcodeValue("")
      return
    }

    setBarcodeError(null)
    barcodeValueRef.current = ""
    setBarcodeValue("")
    addItem(itemId)
    requestAnimationFrame(() => barcodeRef.current?.focus())
  }, [addItem, itemIdByBarcode])

  useEffect(() => {
    function isEditableTarget(target: EventTarget | null) {
      const el = target as HTMLElement | null
      if (!el) return false
      if ((el as unknown as { isContentEditable?: boolean }).isContentEditable) return true
      const tag = (el.tagName || "").toLowerCase()
      return tag === "input" || tag === "textarea" || tag === "select"
    }

    function onKeyDown(e: KeyboardEvent) {
      if (createCustomerOpen) return
      if (isEditableTarget(e.target)) return
      if (e.ctrlKey || e.altKey || e.metaKey) return

      if (e.key === "Enter") {
        e.preventDefault()
        handleBarcodeScanSubmit(barcodeValueRef.current)
        return
      }

      if (e.key === "Backspace") {
        e.preventDefault()
        barcodeValueRef.current = barcodeValueRef.current.slice(0, -1)
        setBarcodeValue(barcodeValueRef.current)
        requestAnimationFrame(() => barcodeRef.current?.focus())
        return
      }

      if (e.key.length === 1) {
        e.preventDefault()
        barcodeValueRef.current = barcodeValueRef.current + e.key
        setBarcodeValue(barcodeValueRef.current)
        setBarcodeError(null)
        requestAnimationFrame(() => barcodeRef.current?.focus())
      }
    }

    window.addEventListener("keydown", onKeyDown, true)
    return () => window.removeEventListener("keydown", onKeyDown, true)
  }, [createCustomerOpen, handleBarcodeScanSubmit])

  function updateLine(index: number, patch: Partial<PosLine>) {
    setLines((prev) => {
      const next = [...prev]
      next[index] = { ...next[index], ...patch }
      return next
    })
  }

  function removeLine(index: number) {
    setLines((prev) => prev.filter((_, i) => i !== index))
  }

  async function submitCreateCustomer() {
    const name = newCustomerName.trim()
    const phone = newCustomerPhone.trim()

    if (!name) {
      setError("Customer name is required")
      return
    }

    setCreatingCustomer(true)
    setError(null)
    try {
      const createdParty = await createParty({
        name,
        type: "CUSTOMER",
        phone: phone.length > 0 ? phone : undefined,
      })
      setParties((prev) => [...prev, createdParty])
      setPartyId(createdParty.id)
      setCreateCustomerOpen(false)
      setNewCustomerName("")
      setNewCustomerPhone("")
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to add customer")
    } finally {
      setCreatingCustomer(false)
    }
  }

  function computeItemCodeBase(name: string) {
    return (
      name
        .trim()
        .toUpperCase()
        .replace(/[^A-Z0-9]+/g, "-")
        .replace(/^-+|-+$/g, "")
        .slice(0, 12) || "ITEM"
    )
  }

  function computeDefaultItemCode(name: string) {
    const base = computeItemCodeBase(name)
    const suffix = String(Date.now()).slice(-4)
    return `${base}-${suffix}`
  }

  function openCreateItemDialog(prefillName?: string) {
    const name = (prefillName ?? "").trim()
    const code = computeDefaultItemCode(name || "ITEM")
    setCreateItemError(null)
    setNewItemCodeEdited(false)
    setNewItemName(name)
    setNewItemCode(code)
    setNewItemBarcode("")
    setNewItemUnitPrice(0)
    setNewItemTaxRate(0)
    setNewItemUom("PCS")
    setCreateItemOpen(true)
  }

  async function submitCreateItem() {
    if (!canCreateItems) {
      setCreateItemError("You don't have permission to add items")
      return
    }

    const name = newItemName.trim()
    const code = newItemCode.trim()
    const barcode = newItemBarcode.trim()
    const uom = newItemUom.trim()
    const unitPrice = Number(newItemUnitPrice)
    const taxRate = Number(newItemTaxRate)

    if (!name) {
      setCreateItemError("Item name is required")
      return
    }
    if (!code) {
      setCreateItemError("Item code is required")
      return
    }
    if (!uom) {
      setCreateItemError("UOM is required")
      return
    }
    if (Number.isNaN(unitPrice) || unitPrice < 0) {
      setCreateItemError("Unit price must be zero or positive")
      return
    }
    if (Number.isNaN(taxRate) || taxRate < 0) {
      setCreateItemError("Tax rate must be zero or positive")
      return
    }

    setCreatingItem(true)
    setCreateItemError(null)
    try {
      const createdItem = await createItem({
        name,
        code,
        barcode: barcode.length > 0 ? barcode : undefined,
        hsnCode: undefined,
        taxRate,
        unitPrice,
        cogs: undefined,
        uom,
        imageUrl: undefined,
        vendorId: undefined,
      })

      setItems((prev) => [createdItem, ...prev])
      setCreateItemOpen(false)
      addItem(createdItem.id)
      requestAnimationFrame(() => barcodeRef.current?.focus())
    } catch (err) {
      setCreateItemError(err instanceof Error ? err.message : "Failed to add item")
    } finally {
      setCreatingItem(false)
    }
  }

  async function createAndPrint() {
    setError(null)
    setCreated(null)
    if (submitting) return

    if (!partyId) {
      setError("Customer is required")
      return
    }
    if (!warehouseId) {
      setError("Warehouse is required")
      return
    }
    if (lines.length === 0) {
      setError("Add at least one item")
      return
    }

    setReceiptDateTime(new Date().toISOString())
    setSubmitting(true)
    const manualDiscountAmount = !appliedOffer && preview.discountAmount > 0 ? preview.discountAmount : 0
    const payload: SalesInvoiceInput = {
      invoiceDate,
      partyId,
      warehouseId,
      invoiceNumber: null,
      offerCode: appliedOffer?.code ?? null,
      offerDiscountAmount: manualDiscountAmount > 0 ? manualDiscountAmount : null,
      lines: lines.map((l) => ({
        itemId: l.itemId,
        quantity: Number(l.quantity),
        unitPrice: Number(l.unitPrice),
      })),
    }

    try {
      const inv = await createSalesInvoice(payload)
      setCreated(inv)
      router.refresh()

      let didReset = false
      const doReset = () => {
        if (didReset) return
        didReset = true
        resetPos()
      }

      const afterPrint = () => {
        window.removeEventListener("afterprint", afterPrint)
        doReset()
      }

      window.addEventListener("afterprint", afterPrint)
      setTimeout(() => window.print(), 50)
      setTimeout(() => {
        window.removeEventListener("afterprint", afterPrint)
        doReset()
      }, 20000)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Create failed")
      setSubmitting(false)
    }
  }

  if (loading) return <div className="p-8 text-center">Loading...</div>

  return (
    <div className="space-y-6">
      <style jsx global>{`
        @media print {
          @page {
            size: 80mm auto;
            margin: 0;
          }

          html,
          body {
            width: 80mm;
            margin: 0 !important;
            padding: 0 !important;
            background: white !important;
          }

          body.pos-print-scope aside,
          body.pos-print-scope header,
          body.pos-print-scope nav,
          body.pos-print-scope .print\\:hidden,
          body.pos-print-scope [role="dialog"] {
            display: none !important;
          }

          body.pos-print-scope main,
          body.pos-print-scope main > div,
          body.pos-print-scope main > div > div {
            max-width: none !important;
            padding: 0 !important;
            margin: 0 !important;
          }

          body.pos-print-scope .pos-receipt-root {
            position: fixed !important;
            left: 0 !important;
            top: 0 !important;
            width: 80mm !important;
            max-width: 80mm !important;
            margin: 0 !important;
            padding: 0 !important;
          }

          body.pos-print-scope .pos-receipt-root * {
            -webkit-print-color-adjust: exact;
            print-color-adjust: exact;
          }
        }
      `}</style>
      <div className="print:hidden">
        <PageHeader
          title="Quick POS"
          description="Fast billing for thermal receipts."
          actions={
            <Link href="/dashboard">
              <Button variant="outline">Back</Button>
            </Link>
          }
        />

        {error ? (
          <div className="rounded-lg border border-destructive/40 bg-destructive/5 p-3 text-sm text-destructive">
            {error}
          </div>
        ) : null}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-4">
        <div className="space-y-4 print:hidden">
          <Card>
            <CardContent className="p-4 space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <div>
                  <div className="text-sm font-medium mb-1">Date</div>
                  <Input value={invoiceDate} type="date" onChange={(e) => setInvoiceDate(e.target.value)} />
                </div>
                <div>
                  <div className="text-sm font-medium mb-1">Customer</div>
                  <SmartSelect<Party>
                    value={partyId}
                    onSelect={(id) => setPartyId(id)}
                    placeholder="Select customer"
                    searchPlaceholder="Search customer..."
                    options={customers}
                    labelKey="name"
                    valueKey="id"
                    renderValue={(p) => <span className="truncate">{p.name}</span>}
                    filterOption={(p, q) => {
                      const hay = `${p.name} ${p.phone ?? ""} ${p.email ?? ""} ${p.gstin ?? ""}`.toLowerCase()
                      return hay.includes(q)
                    }}
                    onCreate={(q) => {
                      setNewCustomerName(q)
                      setNewCustomerPhone("")
                      setCreateCustomerOpen(true)
                    }}
                    createLabel={(q) => `Add customer "${q}"`}
                  />
                </div>
                <div>
                  <div className="text-sm font-medium mb-1">Warehouse</div>
                  <SmartSelect<Warehouse>
                    value={warehouseId}
                    onSelect={(id) => setWarehouseId(id)}
                    placeholder="Select warehouse"
                    searchPlaceholder="Search warehouse..."
                    options={warehouses}
                    labelKey="name"
                    valueKey="id"
                    renderValue={(w) => <span className="truncate">{w.name}</span>}
                    filterOption={(w, q) => w.name.toLowerCase().includes(q)}
                  />
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4 space-y-3">
              <div className="flex items-center justify-between gap-2">
                <div className="text-sm font-medium">Add Item</div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => openCreateItemDialog()}
                  disabled={!canCreateItems}
                >
                  Add New
                </Button>
              </div>
              {canCreateItems === false ? (
                <div className="text-xs text-muted-foreground">Only admins can add new items.</div>
              ) : null}
              <SmartSelect<Item>
                value={undefined}
                onSelect={(id) => {
                  addItem(id)
                  requestAnimationFrame(() => barcodeRef.current?.focus())
                }}
                placeholder="Type to search item"
                searchPlaceholder="Search item by name, code, HSN..."
                options={items}
                labelKey="name"
                valueKey="id"
                onCreate={canCreateItems ? (q) => openCreateItemDialog(q) : undefined}
                createLabel={(q) => `Add item "${q}"`}
                renderOption={(it) => (
                  <div className="flex flex-col">
                    <span className="text-sm">
                      {it.code} - {it.name}
                    </span>
                    <span className="text-xs text-muted-foreground">
                      HSN: {it.hsnCode || "-"} • Price: ₹{Number(it.unitPrice || 0).toLocaleString("en-IN")}
                    </span>
                  </div>
                )}
                renderValue={(it) => (
                  <span className="truncate">
                    {it.code} - {it.name}
                  </span>
                )}
                filterOption={(it, q) => {
                  const hay = `${it.name} ${it.code} ${it.hsnCode ?? ""}`.toLowerCase()
                  return hay.includes(q)
                }}
              />

              <div className="space-y-1">
                <Input
                  ref={barcodeRef}
                  value={barcodeValue}
                  autoFocus
                  placeholder="Scan barcode and press Enter"
                  onChange={(e) => {
                    const v = e.target.value
                    barcodeValueRef.current = v
                    setBarcodeValue(v)
                    setBarcodeError(null)
                  }}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault()
                      handleBarcodeScanSubmit(barcodeValueRef.current)
                    }
                  }}
                />
                {barcodeError ? <div className="text-xs text-destructive">{barcodeError}</div> : null}
              </div>

              <div className="space-y-2">
                {lines.length === 0 ? (
                  <div className="text-sm text-muted-foreground">No items added.</div>
                ) : (
                  lines.map((l, idx) => {
                    const it = itemById.get(l.itemId)
                    return (
                      <div key={`${l.itemId}-${idx}`} className="grid grid-cols-[1fr_90px_120px_32px] gap-2 items-center">
                        <div className="min-w-0">
                          <div className="text-sm font-medium truncate">{it ? it.name : l.itemId}</div>
                          <div className="text-xs text-muted-foreground truncate">{it?.uom ? `UOM: ${it.uom}` : ""}</div>
                        </div>
                        <Input
                          type="number"
                          min={0.01}
                          step={0.01}
                          value={l.quantity}
                          onChange={(e) => updateLine(idx, { quantity: Number(e.target.value) })}
                        />
                        <Input
                          type="number"
                          min={0}
                          step={0.01}
                          value={l.unitPrice}
                          onChange={(e) => updateLine(idx, { unitPrice: Number(e.target.value) })}
                        />
                        <Button variant="ghost" size="icon" onClick={() => removeLine(idx)} aria-label="Remove line">
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    )
                  })
                )}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4 space-y-3">
              <div className="text-sm font-medium">Discount</div>
              <div className="flex gap-2">
                <Button
                  type="button"
                  variant={discountMode === "PERCENT" ? "default" : "outline"}
                  onClick={() => setDiscountMode("PERCENT")}
                  disabled={!!appliedOffer}
                >
                  %
                </Button>
                <Button
                  type="button"
                  variant={discountMode === "FLAT" ? "default" : "outline"}
                  onClick={() => setDiscountMode("FLAT")}
                  disabled={!!appliedOffer}
                >
                  Flat
                </Button>
                <Input
                  type="number"
                  min={0}
                  step="0.01"
                  value={discountValue}
                  onChange={(e) => setDiscountValue(Number(e.target.value))}
                  disabled={!!appliedOffer}
                />
              </div>
              <div className="text-xs text-muted-foreground">
                {appliedOffer
                  ? "Manual discount is disabled while a coupon is applied."
                  : discountMode === "PERCENT"
                    ? "Applies percentage discount to all items."
                    : "Flat discount is distributed across items."}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-4 space-y-3">
              <div className="text-sm font-medium">Coupon</div>
              <div className="flex gap-2">
                <Input
                  placeholder="Enter coupon code"
                  value={couponCode}
                  onChange={(e) => {
                    setCouponCode(e.target.value)
                    setCouponError(null)
                    setAppliedOffer(null)
                  }}
                  disabled={couponLoading}
                />
                {appliedOffer ? (
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setAppliedOffer(null)
                      setCouponError(null)
                      setCouponCode("")
                    }}
                  >
                    Clear
                  </Button>
                ) : (
                  <Button type="button" onClick={() => applyCoupon(couponCode)} disabled={couponLoading}>
                    {couponLoading ? "Applying..." : "Apply"}
                  </Button>
                )}
              </div>
              {couponError ? <div className="text-xs text-destructive">{couponError}</div> : null}
              {appliedOffer ? (
                <div className="text-xs text-muted-foreground">
                  Applied: {appliedOffer.name} (−₹{Number(appliedOffer.discountAmount).toFixed(2)})
                </div>
              ) : null}
            </CardContent>
          </Card>

          <div className="flex items-center justify-between">
            <div className="text-sm text-muted-foreground">
              Total: <span className="text-foreground font-medium">₹{preview.grandTotal.toLocaleString("en-IN")}</span>
            </div>
            <Button onClick={createAndPrint} disabled={submitting}>
              {submitting ? "Creating..." : "Create & Print"}
            </Button>
          </div>
        </div>

        <div className="space-y-3">
          <div className="rounded-lg border bg-card p-3 print:hidden">
            <div className="text-sm font-medium">Receipt Preview</div>
            <div className="text-xs text-muted-foreground">Print uses 80mm width.</div>
          </div>

          <div className="mx-auto w-full print:w-[80mm]">
            <div className="pos-receipt-root w-full rounded-lg border bg-white text-black print:border-0 print:rounded-none">
              <div className="p-3 space-y-2 text-[12px] leading-4">
                <div className="text-center space-y-1">
                  <div className="font-semibold text-[14px]">{organization?.companyName || "Invoice"}</div>
                  <div className="text-[11px]">
                    {organization
                      ? [
                          organization.addressLine1,
                          organization.addressLine2,
                          [organization.city, organization.state, organization.pincode].filter(Boolean).join(" "),
                        ]
                          .filter(Boolean)
                          .join(", ")
                      : ""}
                  </div>
                  <div className="text-[11px]">
                    {organization
                      ? [organization.phone, organization.email, organization.gstin ? `GSTIN: ${organization.gstin}` : ""]
                          .filter(Boolean)
                          .join(" • ")
                      : ""}
                  </div>
                </div>

                <div className="border-t border-b border-dashed py-2 space-y-1">
                  <div className="flex justify-between">
                    <span>Date/Time</span>
                    <span>{receiptDateTimeLabel}</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Bill</span>
                    <span>{created?.invoiceNumber || "-"}</span>
                  </div>
                  <div className="flex justify-between">
                    <span>Customer</span>
                    <span className="max-w-[140px] truncate">{partyById.get(partyId)?.name || "-"}</span>
                  </div>
                </div>

                <div className="space-y-1">
                  <div className="grid grid-cols-[1fr_40px_70px] gap-2 font-medium">
                    <div>Item</div>
                    <div className="text-right">Qty</div>
                    <div className="text-right">Amt</div>
                  </div>
                  {(created?.lines || lines).map((l, idx) => {
                    const it = itemById.get(l.itemId)
                    const qty = Number(l.quantity)
                    const unit = Number(l.unitPrice)
                    const amt = qty * unit
                    return (
                      <div key={`${l.itemId}-${idx}`} className="grid grid-cols-[1fr_40px_70px] gap-2">
                        <div className="truncate">{it ? it.name : l.itemId}</div>
                        <div className="text-right">{qty}</div>
                        <div className="text-right">₹{amt.toFixed(2)}</div>
                      </div>
                    )
                  })}
                </div>

                <div className="border-t border-dashed pt-2 space-y-1">
                  <div className="flex justify-between">
                    <span>Subtotal</span>
                    <span>₹{preview.subtotal.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</span>
                  </div>
                  {(created
                    ? !created.offerCode && Number(created.offerDiscountAmount ?? 0) > 0
                    : preview.discountAmount > 0) ? (
                    <div className="flex justify-between">
                      <span>Manual Discount</span>
                      <span>
                        ₹{(created
                          ? Number(created.offerDiscountAmount ?? 0)
                          : preview.discountAmount
                        ).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                      </span>
                    </div>
                  ) : null}
                  {((created?.offerCode || appliedOffer?.code) &&
                  Number(created?.offerDiscountAmount ?? preview.couponDiscountAmount) > 0) ? (
                    <div className="flex justify-between">
                      <span>Coupon{created?.offerCode ? ` (${created.offerCode})` : appliedOffer?.code ? ` (${appliedOffer.code})` : ""}</span>
                      <span>
                        ₹{(created?.offerDiscountAmount ?? preview.couponDiscountAmount).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                      </span>
                    </div>
                  ) : null}
                  <div className="flex justify-between font-semibold">
                    <span>Total</span>
                    <span>
                      ₹{(created?.grandTotal ?? preview.grandTotal).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </span>
                  </div>
                </div>

                <div className="text-center text-[11px] pt-2">Thank you!</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <Dialog open={createCustomerOpen} onOpenChange={setCreateCustomerOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Add Customer</DialogTitle>
            <DialogDescription>Create a customer for Quick POS.</DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <div>
              <div className="text-sm font-medium mb-1">Name</div>
              <Input value={newCustomerName} onChange={(e) => setNewCustomerName(e.target.value)} />
            </div>
            <div>
              <div className="text-sm font-medium mb-1">Phone (optional)</div>
              <Input value={newCustomerPhone} onChange={(e) => setNewCustomerPhone(e.target.value)} />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateCustomerOpen(false)}>
              Cancel
            </Button>
            <Button onClick={submitCreateCustomer} disabled={creatingCustomer}>
              {creatingCustomer ? "Saving..." : "Save"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={createItemOpen}
        onOpenChange={(open) => {
          setCreateItemOpen(open)
          if (!open) requestAnimationFrame(() => barcodeRef.current?.focus())
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Add Item</DialogTitle>
            <DialogDescription>Create a new item and add it to the bill.</DialogDescription>
          </DialogHeader>
          <div className="space-y-3">
            <div>
              <div className="text-sm font-medium mb-1">Name</div>
              <Input
                value={newItemName}
                onChange={(e) => {
                  const v = e.target.value
                  setNewItemName(v)
                  if (!newItemCodeEdited) {
                    const suffix = newItemCode.split("-").pop() || String(Date.now()).slice(-4)
                    setNewItemCode(`${computeItemCodeBase(v || "ITEM")}-${suffix}`)
                  }
                }}
              />
            </div>
            <div>
              <div className="text-sm font-medium mb-1">Code</div>
              <Input
                value={newItemCode}
                onChange={(e) => {
                  setNewItemCodeEdited(true)
                  setNewItemCode(e.target.value)
                }}
              />
            </div>
            <div>
              <div className="text-sm font-medium mb-1">Barcode (optional)</div>
              <Input value={newItemBarcode} onChange={(e) => setNewItemBarcode(e.target.value)} />
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <div>
                <div className="text-sm font-medium mb-1">UOM</div>
                <Input value={newItemUom} onChange={(e) => setNewItemUom(e.target.value)} />
              </div>
              <div>
                <div className="text-sm font-medium mb-1">Unit Price</div>
                <Input
                  type="number"
                  min={0}
                  step={0.01}
                  value={newItemUnitPrice}
                  onChange={(e) => setNewItemUnitPrice(Number(e.target.value))}
                />
              </div>
              <div>
                <div className="text-sm font-medium mb-1">Tax Rate (%)</div>
                <Input
                  type="number"
                  min={0}
                  step={0.01}
                  value={newItemTaxRate}
                  onChange={(e) => setNewItemTaxRate(Number(e.target.value))}
                />
              </div>
            </div>
            {createItemError ? <div className="text-sm text-destructive">{createItemError}</div> : null}
          </div>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                setCreateItemOpen(false)
                requestAnimationFrame(() => barcodeRef.current?.focus())
              }}
            >
              Cancel
            </Button>
            <Button onClick={submitCreateItem} disabled={creatingItem}>
              {creatingItem ? "Saving..." : "Save & Add"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
