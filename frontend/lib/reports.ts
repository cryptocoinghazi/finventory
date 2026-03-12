
import { apiFetch } from "./api"

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
  vendorId?: string | null
  vendorName?: string | null
  warehouseId: string
  warehouseName: string
  currentStock: number
  uom: string
}

export interface PartyOutstanding {
  partyId: string
  partyName: string
  partyType: string
  phone?: string | null
  gstin?: string | null
  totalReceivable: number
  totalPayable: number
  netBalance: number
  age0to30?: number | null
  age31to60?: number | null
  age61to90?: number | null
  age90Plus?: number | null
}

export type PartyOutstandingStatus = "UNPAID" | "PENDING"
export type PartyOutstandingStatusFilter = "__ALL__" | PartyOutstandingStatus

export function getPartyOutstandingStatus(row: PartyOutstanding): PartyOutstandingStatus {
  const overdue =
    Math.abs(Number(row.age31to60 || 0)) +
    Math.abs(Number(row.age61to90 || 0)) +
    Math.abs(Number(row.age90Plus || 0))
  if (overdue > 0) return "UNPAID"
  return "PENDING"
}

export function matchesPartyOutstandingSearch(row: PartyOutstanding, query: string): boolean {
  const q = query.trim().toLowerCase()
  if (!q) return true
  return (
    row.partyName.toLowerCase().includes(q) ||
    (row.phone || "").toLowerCase().includes(q) ||
    (row.gstin || "").toLowerCase().includes(q)
  )
}

export function filterPartyOutstandingRows(
  rows: PartyOutstanding[],
  params: { query?: string; status?: PartyOutstandingStatusFilter }
): PartyOutstanding[] {
  const query = params.query ?? ""
  const status = params.status ?? "__ALL__"

  return rows.filter((r) => {
    if (!matchesPartyOutstandingSearch(r, query)) return false
    if (status === "__ALL__") return true
    return getPartyOutstandingStatus(r) === status
  })
}

export interface PartyLedgerEntry {
  date: string
  refType: string | null
  refId: string
  description: string | null
  amount: number
}

export interface DashboardStats {
  salesToday: number
  purchaseToday: number
  stockValue: number
  outstanding: number
}

export interface ActivityFeedEntry {
  kind: string
  id: string
  date: string
  title: string
  subtitle: string
  amount: number | null
  href: string | null
}

export interface SystemStatus {
  app: string
  serverTime: string
  dbUp: boolean
  dbError: string | null
  items: number
  parties: number
  warehouses: number
  salesInvoices: number
  purchaseInvoices: number
  salesReturns: number
  purchaseReturns: number
  stockAdjustments: number
  salesInvoicesToday: number
  purchaseInvoicesToday: number
  salesReturnsToday: number
  purchaseReturnsToday: number
  stockAdjustmentsToday: number
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

export async function getPartyOutstanding(params?: {
  fromDate?: string
  toDate?: string
  asOfDate?: string
  partyType?: string
  minOutstanding?: number
}): Promise<PartyOutstanding[]> {
  const qs = new URLSearchParams()
  if (params?.fromDate) qs.set("fromDate", params.fromDate)
  if (params?.toDate) qs.set("toDate", params.toDate)
  if (params?.asOfDate) qs.set("asOfDate", params.asOfDate)
  if (params?.partyType) qs.set("partyType", params.partyType)
  if (params?.minOutstanding != null && Number.isFinite(params.minOutstanding))
    qs.set("minOutstanding", String(params.minOutstanding))

  const res = await apiFetch(
    `/api/reports/party-outstanding${qs.toString() ? `?${qs.toString()}` : ""}`
  )
  if (!res.ok) throw new Error("Failed to fetch party outstanding")
  return res.json()
}

export async function getPartyOutstandingLedger(params: {
  partyId: string
  fromDate?: string
  toDate?: string
}): Promise<PartyLedgerEntry[]> {
  const qs = new URLSearchParams()
  qs.set("partyId", params.partyId)
  if (params.fromDate) qs.set("fromDate", params.fromDate)
  if (params.toDate) qs.set("toDate", params.toDate)

  const res = await apiFetch(`/api/reports/party-outstanding/ledger?${qs.toString()}`)
  if (!res.ok) throw new Error("Failed to fetch party ledger")
  return res.json()
}

export async function getActivityFeed(limit = 10): Promise<ActivityFeedEntry[]> {
  const res = await apiFetch(`/api/reports/activity?limit=${encodeURIComponent(limit)}`)
  if (!res.ok) throw new Error("Failed to fetch activity feed")
  return res.json()
}

export async function getSystemStatus(): Promise<SystemStatus> {
  const res = await apiFetch("/api/reports/system-status", { cache: "no-store" })
  if (!res.ok) throw new Error("Failed to fetch system status")
  return res.json()
}
