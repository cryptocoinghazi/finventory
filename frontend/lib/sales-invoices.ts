import { apiFetch } from "@/lib/api"

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
  lines: SalesInvoiceLine[]
  totalTaxableAmount?: number
  totalTaxAmount?: number
  totalCgstAmount?: number
  totalSgstAmount?: number
  totalIgstAmount?: number
  grandTotal?: number
}

export type SalesInvoiceInput = Omit<SalesInvoice, "id">

export async function listSalesInvoices(): Promise<SalesInvoice[]> {
  const res = await apiFetch("/api/v1/sales-invoices", { cache: "no-store" })
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

