
import { apiFetch } from "@/lib/api"

export interface SalesReturnLine {
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

export interface SalesReturn {
  id: string
  returnNumber: string
  salesInvoiceId?: string
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
  lines: SalesReturnLine[]
}

export interface SalesReturnInput {
  returnNumber: string
  salesInvoiceId?: string
  returnDate: string
  partyId: string
  warehouseId: string
  lines: {
    itemId: string
    quantity: number
    unitPrice: number
  }[]
}

export async function getSalesReturns(): Promise<SalesReturn[]> {
  const res = await apiFetch("/api/sales-returns", { cache: "no-store" })
  if (!res.ok) throw new Error("Failed to fetch sales returns")
  return res.json()
}

export async function getSalesReturn(id: string): Promise<SalesReturn> {
  const res = await apiFetch(`/api/sales-returns/${id}`, { cache: "no-store" })
  if (!res.ok) throw new Error("Failed to fetch sales return")
  return res.json()
}

export async function createSalesReturn(data: SalesReturnInput): Promise<SalesReturn> {
  const res = await apiFetch("/api/sales-returns", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error("Failed to create sales return")
  return res.json()
}
