import { apiFetch } from "./api"

export interface TaxSlab {
  id: string
  rate: number
  description: string
}

export type TaxSlabInput = Omit<TaxSlab, "id">

export async function listTaxSlabs(): Promise<TaxSlab[]> {
  const res = await apiFetch("/api/v1/tax-slabs")
  if (!res.ok) throw new Error("Failed to fetch tax slabs")
  return res.json()
}

export async function getTaxSlab(id: string): Promise<TaxSlab> {
  const res = await apiFetch(`/api/v1/tax-slabs/${id}`)
  if (!res.ok) throw new Error("Failed to fetch tax slab")
  return res.json()
}

export async function createTaxSlab(data: TaxSlabInput): Promise<TaxSlab> {
  const res = await apiFetch("/api/v1/tax-slabs", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error("Failed to create tax slab")
  return res.json()
}

export async function updateTaxSlab(id: string, data: TaxSlabInput): Promise<TaxSlab> {
  const res = await apiFetch(`/api/v1/tax-slabs/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error("Failed to update tax slab")
  return res.json()
}

export async function deleteTaxSlab(id: string): Promise<void> {
  const res = await apiFetch(`/api/v1/tax-slabs/${id}`, {
    method: "DELETE",
  })
  if (!res.ok) throw new Error("Failed to delete tax slab")
}
