import { apiFetch } from "@/lib/api"

export type StockSummary = {
  itemId: string
  itemName: string
  itemCode: string
  warehouseId: string
  warehouseName: string
  currentStock: number
  uom: string
}

export type PartyOutstanding = {
  partyId: string
  partyName: string
  partyType: string
  totalReceivable: number
  totalPayable: number
  netBalance: number
}

export async function getStockSummary(): Promise<StockSummary[]> {
  const res = await apiFetch("/api/reports/stock-summary", { cache: "no-store" })
  return readJsonOrThrow<StockSummary[]>(res)
}

export async function getPartyOutstanding(): Promise<PartyOutstanding[]> {
  const res = await apiFetch("/api/reports/party-outstanding", { cache: "no-store" })
  return readJsonOrThrow<PartyOutstanding[]>(res)
}

async function readJsonOrThrow<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const msg = await safeReadText(res)
    throw new Error(msg || `Request failed (${res.status})`)
  }
  return (await res.json()) as T
}

async function safeReadText(res: Response): Promise<string> {
  try {
    return await res.text()
  } catch {
    return ""
  }
}

