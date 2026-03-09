import { API_BASE } from "./api"

export interface PurchaseReturnLine {
  id: string
  itemId: string
  itemName?: string
  quantity: number
  unitPrice: number
  taxRate?: number
  taxAmount: number
  lineTotal: number
}

export interface PurchaseReturn {
  id: string
  returnNumber: string
  returnDate: string
  partyId: string
  partyName?: string
  warehouseId: string
  warehouseName?: string
  totalTaxableAmount: number
  totalTaxAmount: number
  grandTotal: number
  lines: PurchaseReturnLine[]
}

export async function listPurchaseReturns(): Promise<PurchaseReturn[]> {
  const res = await fetch(`${API_BASE}/api/purchase-returns`, {
    headers: {
      Authorization: `Bearer ${window.localStorage.getItem("token")}`,
    },
  })
  if (!res.ok) throw new Error("Failed to fetch purchase returns")
  return res.json()
}
