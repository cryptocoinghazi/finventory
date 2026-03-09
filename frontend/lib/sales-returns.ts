import { API_BASE } from "./api"

export interface SalesReturnLine {
  id: string
  itemId: string
  itemName?: string
  quantity: number
  unitPrice: number
  taxRate?: number
  taxAmount: number
  lineTotal: number
}

export interface SalesReturn {
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
  lines: SalesReturnLine[]
}

export async function listSalesReturns(): Promise<SalesReturn[]> {
  const res = await fetch(`${API_BASE}/api/sales-returns`, {
    headers: {
      Authorization: `Bearer ${window.localStorage.getItem("token")}`,
    },
  })
  if (!res.ok) throw new Error("Failed to fetch sales returns")
  return res.json()
}
