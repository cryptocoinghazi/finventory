
import { apiFetch } from "@/lib/api"

export interface PurchaseReturnLine {
  id?: string
  itemId: string
  itemName?: string
  itemCode?: string
  quantity: number
  unitPrice: number
  taxRate?: number
  taxAmount?: number
  lineTotal?: number
}

export interface PurchaseReturn {
  id: string
  returnNumber: string
  purchaseInvoiceId?: string
  returnDate: string
  partyId: string
  partyName?: string
  warehouseId: string
  warehouseName?: string
  totalTaxableAmount: number
  totalTaxAmount: number
  totalCgstAmount: number
  totalSgstAmount: number
  totalIgstAmount: number
  grandTotal: number
  lines: PurchaseReturnLine[]
}

export interface PurchaseReturnInput {
  returnNumber: string
  purchaseInvoiceId?: string
  returnDate: string
  partyId: string
  warehouseId: string
  lines: {
    itemId: string
    quantity: number
    unitPrice: number
  }[]
}

export async function getPurchaseReturns(): Promise<PurchaseReturn[]> {
  const res = await apiFetch("/api/purchase-returns", { cache: "no-store" })
  if (!res.ok) throw new Error("Failed to fetch purchase returns")
  return res.json()
}

export async function getPurchaseReturn(id: string): Promise<PurchaseReturn> {
  const res = await apiFetch(`/api/purchase-returns/${id}`, { cache: "no-store" })
  if (!res.ok) throw new Error("Failed to fetch purchase return")
  return res.json()
}

export async function createPurchaseReturn(data: PurchaseReturnInput): Promise<PurchaseReturn> {
  const res = await apiFetch("/api/purchase-returns", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error("Failed to create purchase return")
  return res.json()
}
