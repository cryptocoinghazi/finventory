import { apiFetch } from "@/lib/api"

export type InvoicePaymentStatus = "PENDING" | "PARTIAL" | "PAID"

export type PurchaseInvoiceLine = {
  id?: string
  itemId: string
  itemName?: string
  itemCode?: string
  quantity: number
  unitPrice: number
  taxRate?: number
  taxAmount?: number
  cgstAmount?: number
  sgstAmount?: number
  igstAmount?: number
  lineTotal?: number
}

export type PurchaseInvoice = {
  id: string
  invoiceDate: string
  partyId: string
  partyName?: string
  warehouseId: string
  warehouseName?: string
  invoiceNumber?: string | null
  vendorInvoiceNumber?: string | null
  paymentStatus?: InvoicePaymentStatus
  paidAmount?: number
  balanceAmount?: number
  cancelledAt?: string | null
  deletedAt?: string | null
  cancelReason?: string | null
  lines: PurchaseInvoiceLine[]
  totalTaxableAmount?: number
  totalTaxAmount?: number
  totalCgstAmount?: number
  totalSgstAmount?: number
  totalIgstAmount?: number
  grandTotal?: number
}

export type PurchaseInvoiceInput = Omit<PurchaseInvoice, "id">

export async function listPurchaseInvoices(filters?: {
  paymentStatus?: InvoicePaymentStatus | ""
  fromDate?: string
  toDate?: string
}): Promise<PurchaseInvoice[]> {
  const params = new URLSearchParams()
  if (filters?.paymentStatus) params.set("paymentStatus", filters.paymentStatus)
  if (filters?.fromDate) params.set("fromDate", filters.fromDate)
  if (filters?.toDate) params.set("toDate", filters.toDate)
  const url = params.toString()
    ? `/api/v1/purchase-invoices?${params.toString()}`
    : "/api/v1/purchase-invoices"
  const res = await apiFetch(url, { cache: "no-store" })
  return readJsonOrThrow<PurchaseInvoice[]>(res)
}

export async function getPurchaseInvoice(id: string): Promise<PurchaseInvoice> {
  const res = await apiFetch(`/api/v1/purchase-invoices/${encodeURIComponent(id)}`, {
    cache: "no-store",
  })
  return readJsonOrThrow<PurchaseInvoice>(res)
}

export async function createPurchaseInvoice(input: PurchaseInvoiceInput): Promise<PurchaseInvoice> {
  const res = await apiFetch("/api/v1/purchase-invoices", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  })
  return readJsonOrThrow<PurchaseInvoice>(res)
}

export async function updatePurchaseInvoicePaymentStatus(
  id: string,
  paymentStatus: InvoicePaymentStatus
): Promise<PurchaseInvoice> {
  const res = await apiFetch(`/api/v1/purchase-invoices/${encodeURIComponent(id)}/payment-status`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ paymentStatus }),
  })
  return readJsonOrThrow<PurchaseInvoice>(res)
}

export async function applyPurchaseInvoicePayment(
  id: string,
  paymentStatus: InvoicePaymentStatus,
  paymentAmount: number
): Promise<PurchaseInvoice> {
  const res = await apiFetch(`/api/v1/purchase-invoices/${encodeURIComponent(id)}/payment`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ paymentStatus, paymentAmount }),
  })
  return readJsonOrThrow<PurchaseInvoice>(res)
}

export async function cancelPurchaseInvoice(id: string, reason?: string): Promise<PurchaseInvoice> {
  const params = new URLSearchParams()
  if (reason) params.set("reason", reason)
  const url = params.toString()
    ? `/api/v1/purchase-invoices/${encodeURIComponent(id)}?${params.toString()}`
    : `/api/v1/purchase-invoices/${encodeURIComponent(id)}`
  const res = await apiFetch(url, { method: "DELETE" })
  return readJsonOrThrow<PurchaseInvoice>(res)
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
