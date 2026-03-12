import { apiFetch } from "@/lib/api"

export type OfferDiscountType = "PERCENT" | "FLAT"
export type OfferScope = "CART" | "ITEM"

export type Offer = {
  id: string
  name: string
  code?: string | null
  discountType: OfferDiscountType
  scope: OfferScope
  discountValue: number
  itemId?: string | null
  startDate?: string | null
  endDate?: string | null
  active: boolean
  usageLimit?: number | null
  usedCount?: number | null
  minBillAmount?: number | null
}

export type OfferInput = Omit<Offer, "id" | "usedCount">

export type OfferValidationLine = {
  itemId: string
  quantity: number
  unitPrice: number
}

export type OfferValidationRequest = {
  code: string
  asOfDate?: string
  taxableSubtotal: number
  lines: OfferValidationLine[]
}

export type OfferValidationResponse = {
  offerId: string
  code: string
  name: string
  scope: OfferScope
  discountType: OfferDiscountType
  discountValue: number
  discountAmount: number
}

export async function listOffers(opts?: { active?: boolean; asOfDate?: string }): Promise<Offer[]> {
  const params = new URLSearchParams()
  if (opts?.active) params.set("active", "true")
  if (opts?.asOfDate) params.set("asOfDate", opts.asOfDate)

  const url = params.toString() ? `/api/v1/offers?${params.toString()}` : "/api/v1/offers"
  const res = await apiFetch(url, { cache: "no-store" })
  return readJsonOrThrow<Offer[]>(res)
}

export async function getOffer(id: string): Promise<Offer> {
  const res = await apiFetch(`/api/v1/offers/${encodeURIComponent(id)}`, { cache: "no-store" })
  return readJsonOrThrow<Offer>(res)
}

export async function createOffer(input: OfferInput): Promise<Offer> {
  const res = await apiFetch("/api/v1/offers", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  })
  return readJsonOrThrow<Offer>(res)
}

export async function updateOffer(id: string, input: OfferInput): Promise<Offer> {
  const res = await apiFetch(`/api/v1/offers/${encodeURIComponent(id)}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  })
  return readJsonOrThrow<Offer>(res)
}

export async function deleteOffer(id: string): Promise<void> {
  const res = await apiFetch(`/api/v1/offers/${encodeURIComponent(id)}`, { method: "DELETE" })
  if (!res.ok) {
    const msg = await safeReadText(res)
    throw new Error(msg || `Delete failed (${res.status})`)
  }
}

export async function validateOffer(input: OfferValidationRequest): Promise<OfferValidationResponse> {
  const res = await apiFetch("/api/v1/offers/validate", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  })
  return readJsonOrThrow<OfferValidationResponse>(res)
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

