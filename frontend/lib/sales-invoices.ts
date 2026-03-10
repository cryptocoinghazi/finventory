import { apiFetch } from "@/lib/api"

export type InvoicePaymentStatus = "PENDING" | "PARTIAL" | "PAID"

export type SalesInvoiceLine = {
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

export type SalesInvoice = {
  id: string
  invoiceDate: string
  partyId: string
  partyName?: string
  warehouseId: string
  warehouseName?: string
  invoiceNumber?: string | null
  paymentStatus?: InvoicePaymentStatus
  paidAmount?: number
  balanceAmount?: number
  lines: SalesInvoiceLine[]
  totalTaxableAmount?: number
  totalTaxAmount?: number
  totalCgstAmount?: number
  totalSgstAmount?: number
  totalIgstAmount?: number
  grandTotal?: number
}

export type SalesInvoiceInput = Omit<SalesInvoice, "id">

export async function listSalesInvoices(filters?: {
  paymentStatus?: InvoicePaymentStatus | ""
  fromDate?: string
  toDate?: string
}): Promise<SalesInvoice[]> {
  const params = new URLSearchParams()
  if (filters?.paymentStatus) params.set("paymentStatus", filters.paymentStatus)
  if (filters?.fromDate) params.set("fromDate", filters.fromDate)
  if (filters?.toDate) params.set("toDate", filters.toDate)
  const url = params.toString() ? `/api/v1/sales-invoices?${params.toString()}` : "/api/v1/sales-invoices"
  const res = await apiFetch(url, { cache: "no-store" })
  return readJsonOrThrow<SalesInvoice[]>(res)
}

export async function getSalesInvoice(id: string): Promise<SalesInvoice> {
  const res = await apiFetch(`/api/v1/sales-invoices/${encodeURIComponent(id)}`, {
    cache: "no-store",
  })
  return readJsonOrThrow<SalesInvoice>(res)
}

export async function createSalesInvoice(input: SalesInvoiceInput): Promise<SalesInvoice> {
  const res = await apiFetch("/api/v1/sales-invoices", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  })
  return readJsonOrThrow<SalesInvoice>(res)
}

export async function updateSalesInvoicePaymentStatus(
  id: string,
  paymentStatus: InvoicePaymentStatus
): Promise<SalesInvoice> {
  const res = await apiFetch(`/api/v1/sales-invoices/${encodeURIComponent(id)}/payment-status`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ paymentStatus }),
  })
  return readJsonOrThrow<SalesInvoice>(res)
}

export async function applySalesInvoicePayment(
  id: string,
  paymentStatus: InvoicePaymentStatus,
  paymentAmount: number
): Promise<SalesInvoice> {
  const res = await apiFetch(`/api/v1/sales-invoices/${encodeURIComponent(id)}/payment`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ paymentStatus, paymentAmount }),
  })
  return readJsonOrThrow<SalesInvoice>(res)
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
