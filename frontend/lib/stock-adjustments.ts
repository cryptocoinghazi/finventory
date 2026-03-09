import { apiFetch } from "@/lib/api"

export interface StockAdjustment {
  id: string
  adjustmentNumber: string
  adjustmentDate: string
  warehouseId: string
  warehouseName: string
  itemId: string
  itemName: string
  quantity: number
  reason?: string
}

export type StockAdjustmentInput = Omit<
  StockAdjustment,
  "id" | "adjustmentNumber" | "warehouseName" | "itemName"
>

export async function createStockAdjustment(
  input: StockAdjustmentInput
): Promise<StockAdjustment> {
  const res = await apiFetch("/api/v1/stock-adjustments", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  })
  if (!res.ok) {
    const msg = await res.text()
    throw new Error(msg || "Failed to create adjustment")
  }
  return res.json()
}
