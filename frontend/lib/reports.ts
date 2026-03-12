
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

export type ReportInvoiceStatus = "ACTIVE" | "CANCELLED" | "DELETED" | "ALL"
export type SalesGroupBy = "DAY" | "WEEK" | "MONTH" | "RANGE"
export type InvoicePaymentStatus = "PENDING" | "PARTIAL" | "PAID"

export interface SalesReportTotals {
  invoiceCount: number
  totalAmount: number
  totalDiscount: number
  totalPaid: number
  totalPending: number
}

export interface SalesReportBucket {
  periodStart: string | null
  periodEnd: string | null
  label: string | null
  invoiceCount: number
  totalAmount: number
  totalDiscount: number
  totalPaid: number
  totalPending: number
}

export interface SalesReportResponse {
  fromDate: string | null
  toDate: string | null
  groupBy: string | null
  totals: SalesReportTotals
  buckets: SalesReportBucket[]
}

export async function getSalesReport(params?: {
  fromDate?: string
  toDate?: string
  groupBy?: SalesGroupBy
  itemId?: string
  category?: string
  partyId?: string
  paymentStatus?: InvoicePaymentStatus
  invoiceStatus?: ReportInvoiceStatus
}): Promise<SalesReportResponse> {
  const qs = new URLSearchParams()
  if (params?.fromDate) qs.set("fromDate", params.fromDate)
  if (params?.toDate) qs.set("toDate", params.toDate)
  if (params?.groupBy) qs.set("groupBy", params.groupBy)
  if (params?.itemId) qs.set("itemId", params.itemId)
  if (params?.category) qs.set("category", params.category)
  if (params?.partyId) qs.set("partyId", params.partyId)
  if (params?.paymentStatus) qs.set("paymentStatus", params.paymentStatus)
  if (params?.invoiceStatus) qs.set("invoiceStatus", params.invoiceStatus)
  const res = await apiFetch(`/api/reports/sales${qs.toString() ? `?${qs.toString()}` : ""}`, {
    cache: "no-store",
  })
  if (!res.ok) throw new Error("Failed to fetch sales report")
  return res.json()
}

export interface StockReportRow {
  itemId: string
  itemName: string
  itemCode: string
  vendorId?: string | null
  vendorName?: string | null
  warehouseId: string
  warehouseName: string
  currentStock: number
  uom: string
  unitPrice: number
  valuation: number
}

export interface LowStockResponse {
  threshold: number
  lowStock: StockReportRow[]
  outOfStock: StockReportRow[]
}

export interface StockMovementEntry {
  date: string
  itemId: string
  itemName: string
  warehouseId: string
  warehouseName: string
  qtyIn: number
  qtyOut: number
  refType: string | null
  refId: string
}

export async function getStockReport(): Promise<StockReportRow[]> {
  const res = await apiFetch("/api/reports/stock", { cache: "no-store" })
  if (!res.ok) throw new Error("Failed to fetch stock report")
  return res.json()
}

export async function getLowStockReport(params?: { threshold?: number }): Promise<LowStockResponse> {
  const qs = new URLSearchParams()
  if (params?.threshold != null && Number.isFinite(params.threshold))
    qs.set("threshold", String(params.threshold))
  const res = await apiFetch(`/api/reports/stock/low${qs.toString() ? `?${qs.toString()}` : ""}`, {
    cache: "no-store",
  })
  if (!res.ok) throw new Error("Failed to fetch low stock report")
  return res.json()
}

export async function getStockMovement(params?: {
  fromDate?: string
  toDate?: string
  itemId?: string
  warehouseId?: string
}): Promise<StockMovementEntry[]> {
  const qs = new URLSearchParams()
  if (params?.fromDate) qs.set("fromDate", params.fromDate)
  if (params?.toDate) qs.set("toDate", params.toDate)
  if (params?.itemId) qs.set("itemId", params.itemId)
  if (params?.warehouseId) qs.set("warehouseId", params.warehouseId)
  const res = await apiFetch(
    `/api/reports/stock/movement${qs.toString() ? `?${qs.toString()}` : ""}`,
    { cache: "no-store" }
  )
  if (!res.ok) throw new Error("Failed to fetch stock movement")
  return res.json()
}

export interface ProfitLossReport {
  fromDate: string | null
  toDate: string | null
  revenue: number
  discounts: number
  expenses: number
  cogs: number
  grossProfit: number
  netProfit: number
}

export async function getProfitLossReport(params?: {
  fromDate?: string
  toDate?: string
}): Promise<ProfitLossReport> {
  const qs = new URLSearchParams()
  if (params?.fromDate) qs.set("fromDate", params.fromDate)
  if (params?.toDate) qs.set("toDate", params.toDate)
  const res = await apiFetch(
    `/api/reports/profit-loss${qs.toString() ? `?${qs.toString()}` : ""}`,
    { cache: "no-store" }
  )
  if (!res.ok) throw new Error("Failed to fetch profit & loss report")
  return res.json()
}

export interface AnnualReportMonthTrend {
  month: number
  revenue: number
  discounts: number
  expenses: number
  cogs: number
  grossProfit: number
  netProfit: number
}

export interface AnnualReportTopItem {
  itemId: string
  itemName: string
  itemCode: string
  quantity: number
  amount: number
}

export interface AnnualReportTopParty {
  partyId: string
  partyName: string
  amount: number
  invoiceCount: number
}

export interface AnnualReport {
  year: number
  months: AnnualReportMonthTrend[]
  topItems: AnnualReportTopItem[]
  topCustomers: AnnualReportTopParty[]
}

export async function getAnnualReport(params: { year: number; topLimit?: number }): Promise<AnnualReport> {
  const qs = new URLSearchParams()
  qs.set("year", String(params.year))
  if (params.topLimit != null && Number.isFinite(params.topLimit)) qs.set("topLimit", String(params.topLimit))
  const res = await apiFetch(`/api/reports/annual?${qs.toString()}`, { cache: "no-store" })
  if (!res.ok) {
    let text = ""
    try {
      text = await res.text()
    } catch {}
    const msg = text?.trim()
    throw new Error(msg || `Failed to fetch annual report (${res.status})`)
  }
  return res.json()
}
