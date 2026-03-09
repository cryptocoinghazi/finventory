
import { apiFetch } from "@/lib/api"

export interface GstRegisterEntry {
  invoiceNumber: string
  invoiceDate: string
  partyName: string
  partyGstin: string
  placeOfSupply: string
  invoiceType: string
  taxableValue: number
  cgstAmount: number
  sgstAmount: number
  igstAmount: number
  totalAmount: number
}

export interface Gstr3b {
  outwardTaxableValue: number
  outwardIgst: number
  outwardCgst: number
  outwardSgst: number
  itcIgst: number
  itcCgst: number
  itcSgst: number
  netTaxPayable: number
}

export interface StockSummary {
  itemId: string
  itemName: string
  itemCode: string
  warehouseId: string
  warehouseName: string
  currentStock: number
  uom: string
}

export interface PartyOutstanding {
  partyId: string
  partyName: string
  partyType: string
  totalReceivable: number
  totalPayable: number
  netBalance: number
}

export interface DashboardStats {
  salesToday: number
  purchaseToday: number
  stockValue: number
  outstanding: number
}

export async function getDashboardStats(): Promise<DashboardStats> {
  const res = await apiFetch("/api/reports/dashboard-stats")
  if (!res.ok) throw new Error("Failed to fetch dashboard stats")
  return res.json()
}

export async function getGstr1(): Promise<GstRegisterEntry[]> {
  const res = await apiFetch("/api/reports/gstr-1")
  if (!res.ok) throw new Error("Failed to fetch GSTR-1")
  return res.json()
}

export async function getGstr2(): Promise<GstRegisterEntry[]> {
  const res = await apiFetch("/api/reports/gstr-2")
  if (!res.ok) throw new Error("Failed to fetch GSTR-2")
  return res.json()
}

export async function getGstr3b(): Promise<Gstr3b> {
  const res = await apiFetch("/api/reports/gstr-3b")
  if (!res.ok) throw new Error("Failed to fetch GSTR-3B")
  return res.json()
}

export async function getStockSummary(): Promise<StockSummary[]> {
  const res = await apiFetch("/api/reports/stock-summary")
  if (!res.ok) throw new Error("Failed to fetch stock summary")
  return res.json()
}

export async function getPartyOutstanding(): Promise<PartyOutstanding[]> {
  const res = await apiFetch("/api/reports/party-outstanding")
  if (!res.ok) throw new Error("Failed to fetch party outstanding")
  return res.json()
}
