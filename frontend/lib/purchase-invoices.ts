import { apiFetch } from "@/lib/api"

export type PurchaseInvoiceLine = {
  id?: string
  itemId: string
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
  warehouseId: string
  invoiceNumber?: string | null
  vendorInvoiceNumber?: string | null
  lines: PurchaseInvoiceLine[]
  totalTaxableAmount?: number
  totalTaxAmount?: number
  totalCgstAmount?: number
  totalSgstAmount?: number
  totalIgstAmount?: number
  grandTotal?: number
}

export type PurchaseInvoiceInput = Omit<PurchaseInvoice, "id">

export async function listPurchaseInvoices(): Promise<PurchaseInvoice[]> {
  const res = await apiFetch("/api/v1/purchase-invoices", { cache: "no-store" })
  return readJsonOrThrow<PurchaseInvoice[]>(res)
}

export async function getPurchaseInvoice(id: string): Promise<PurchaseInvoice> {
  const res = await apiFetch(`/api/v1/purchase-invoices/${encodeURIComponent(id)}`, {
    cache: "no-store",
  })
  return readJsonOrThrow<PurchaseInvoice>(res)
}

export async function createPurchaseInvoice(
  input: PurchaseInvoiceInput,
): Promise<PurchaseInvoice> {
  const res = await apiFetch("/api/v1/purchase-invoices", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  })
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
