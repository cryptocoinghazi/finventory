import { API_BASE } from "./api"

export interface TaxSlab {
  id: string
  rate: number
  description: string
}

export type TaxSlabInput = Omit<TaxSlab, "id">

export async function listTaxSlabs(): Promise<TaxSlab[]> {
  const res = await fetch(`${API_BASE}/api/v1/tax-slabs`, {
    headers: {
      Authorization: `Bearer ${window.localStorage.getItem("token")}`,
    },
  })
  if (!res.ok) throw new Error("Failed to fetch tax slabs")
  return res.json()
}

export async function createTaxSlab(data: TaxSlabInput): Promise<TaxSlab> {
  const res = await fetch(`${API_BASE}/api/v1/tax-slabs`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${window.localStorage.getItem("token")}`,
    },
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error("Failed to create tax slab")
  return res.json()
}

export async function deleteTaxSlab(id: string): Promise<void> {
  const res = await fetch(`${API_BASE}/api/v1/tax-slabs/${id}`, {
    method: "DELETE",
    headers: {
      Authorization: `Bearer ${window.localStorage.getItem("token")}`,
    },
  })
  if (!res.ok) throw new Error("Failed to delete tax slab")
}
