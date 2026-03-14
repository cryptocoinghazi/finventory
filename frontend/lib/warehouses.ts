import { apiFetch } from "@/lib/api"

export type Warehouse = {
  id: string
  name: string
  stateCode?: string | null
  location?: string | null
}

export type WarehouseInput = Omit<Warehouse, "id">

export async function listWarehouses(): Promise<Warehouse[]> {
  const res = await apiFetch("/api/v1/warehouses", { cache: "no-store" })
  return readJsonOrThrow<Warehouse[]>(res)
}

export async function getWarehouse(id: string): Promise<Warehouse> {
  const res = await apiFetch(`/api/v1/warehouses/${encodeURIComponent(id)}`, {
    cache: "no-store",
  })
  return readJsonOrThrow<Warehouse>(res)
}

export async function createWarehouse(input: WarehouseInput): Promise<Warehouse> {
  const res = await apiFetch("/api/v1/warehouses", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  })
  return readJsonOrThrow<Warehouse>(res)
}

export async function updateWarehouse(id: string, input: WarehouseInput): Promise<Warehouse> {
  const res = await apiFetch(`/api/v1/warehouses/${encodeURIComponent(id)}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  })
  return readJsonOrThrow<Warehouse>(res)
}

export async function deleteWarehouse(id: string): Promise<void> {
  const res = await apiFetch(`/api/v1/warehouses/${encodeURIComponent(id)}`, {
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

