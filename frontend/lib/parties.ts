import { apiFetch } from "@/lib/api"

export type PartyType = "CUSTOMER" | "VENDOR"

export type Party = {
  id: string
  name: string
  type: PartyType
  gstin?: string | null
  stateCode?: string | null
  address?: string | null
  phone?: string | null
  email?: string | null
}

export type PartyInput = Omit<Party, "id">

export async function listParties(type?: PartyType): Promise<Party[]> {
  const params = new URLSearchParams()
  if (type) params.set("type", type)
  const url = params.toString() ? `/api/v1/parties?${params.toString()}` : "/api/v1/parties"
  const res = await apiFetch(url, { cache: "no-store" })
  return readJsonOrThrow<Party[]>(res)
}

export async function getParty(id: string): Promise<Party> {
  const res = await apiFetch(`/api/v1/parties/${encodeURIComponent(id)}`, {
    cache: "no-store",
  })
  return readJsonOrThrow<Party>(res)
}

export async function createParty(input: PartyInput): Promise<Party> {
  const res = await apiFetch("/api/v1/parties", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  })
  return readJsonOrThrow<Party>(res)
}

export async function updateParty(id: string, input: PartyInput): Promise<Party> {
  const res = await apiFetch(`/api/v1/parties/${encodeURIComponent(id)}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  })
  return readJsonOrThrow<Party>(res)
}

export async function deleteParty(id: string): Promise<void> {
  const res = await apiFetch(`/api/v1/parties/${encodeURIComponent(id)}`, {
    method: "DELETE",
  })
  if (!res.ok) {
    const msg = await safeReadText(res)
    throw new Error(msg || `Delete failed (${res.status})`)
  }
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
