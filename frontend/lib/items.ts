import { apiFetch } from "@/lib/api"

export type Item = {
  id: string
  name: string
  code: string
  category?: string | null
  barcode?: string | null
  hsnCode?: string | null
  taxRate: number
  unitPrice: number
  cogs?: number | null
  uom: string
  imageUrl?: string | null
  vendorId?: string | null
  vendorName?: string | null
}

export type ItemInput = Omit<Item, "id" | "vendorName">

export type BulkItemUploadRowStatus =
  | "CREATED"
  | "SKIPPED"
  | "FAILED"
  | "CREATED_WITH_WARNING"

export type BulkItemUploadRowResult = {
  rowNumber: number
  status: BulkItemUploadRowStatus
  message: string
  itemId?: string | null
  warning?: string | null
}

export type BulkItemCreatedWithoutImage = {
  itemId: string
  code: string
  name: string
  vendorName?: string | null
}

export type BulkItemUploadResponse = {
  totalRows: number
  createdCount: number
  skippedCount: number
  failedCount: number
  details: BulkItemUploadRowResult[]
  missingImages: BulkItemCreatedWithoutImage[]
}

export async function listItems(): Promise<Item[]> {
  const res = await apiFetch("/api/v1/items", { cache: "no-store" })
  return readJsonOrThrow<Item[]>(res)
}

export async function getItem(id: string): Promise<Item> {
  const res = await apiFetch(`/api/v1/items/${encodeURIComponent(id)}`, {
    cache: "no-store",
  })
  return readJsonOrThrow<Item>(res)
}

export async function uploadItems(
  formData: FormData
): Promise<BulkItemUploadResponse> {
  const res = await apiFetch("/api/v1/items/upload?report=true", {
    method: "POST",
    body: formData,
  })
  return readJsonOrThrow<BulkItemUploadResponse>(res)
}

export async function uploadItemImage(id: string, file: File): Promise<Item> {
  const formData = new FormData()
  formData.append("file", file)
  const res = await apiFetch(`/api/v1/items/${encodeURIComponent(id)}/image`, {
    method: "POST",
    body: formData,
  })
  return readJsonOrThrow<Item>(res)
}

export async function createItem(input: ItemInput): Promise<Item> {
  const res = await apiFetch("/api/v1/items", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  })
  return readJsonOrThrow<Item>(res)
}

export async function updateItem(id: string, input: ItemInput): Promise<Item> {
  const res = await apiFetch(`/api/v1/items/${encodeURIComponent(id)}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  })
  return readJsonOrThrow<Item>(res)
}

export async function deleteItem(id: string): Promise<void> {
  const res = await apiFetch(`/api/v1/items/${encodeURIComponent(id)}`, {
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
