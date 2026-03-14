import { apiFetch } from "@/lib/api"

export type LabelBarcodeFormat = "AUTO" | "CODE128" | "EAN13"
export type LabelTemplateName = "LABEL_2X1" | "LABEL_2X2" | "LABEL_4X2"
export type LabelPrintJobStatus =
  | "PREPARED"
  | "PRINT_DIALOG_OPENED"
  | "PRINTED"
  | "CANCELLED"
  | "FAILED_VALIDATION"

export type LabelPrintPrepareLineRequest = {
  itemId: string
  quantity: number
}

export type LabelPrintPreparedItem = {
  itemId: string
  name: string
  code: string
  barcode: string
  unitPrice: number
  quantity: number
  effectiveBarcodeFormat: LabelBarcodeFormat
}

export type LabelPrintInvalidItem = {
  itemId: string
  name?: string | null
  code?: string | null
  barcode?: string | null
  unitPrice?: number | null
  quantity: number
  errors: string[]
}

export type LabelPrintPrepareResponse = {
  jobId: string
  status: LabelPrintJobStatus
  templateName: LabelTemplateName
  requestedBarcodeFormat: LabelBarcodeFormat
  includeItemCode: boolean
  totalLabelsRequested: number
  totalLabelsValid: number
  items: LabelPrintPreparedItem[]
  invalidItems: LabelPrintInvalidItem[]
}

export async function prepareLabelPrintJob(input: {
  items: LabelPrintPrepareLineRequest[]
  templateName: LabelTemplateName
  barcodeFormat: LabelBarcodeFormat
  includeItemCode: boolean
}): Promise<LabelPrintPrepareResponse> {
  const res = await apiFetch("/api/v1/labels/print-jobs/prepare", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  })
  if (!res.ok) {
    const msg = await safeReadText(res)
    throw new Error(msg || `Prepare failed (${res.status})`)
  }
  return (await res.json()) as LabelPrintPrepareResponse
}

export async function updateLabelPrintJobStatus(
  jobId: string,
  status: LabelPrintJobStatus
): Promise<void> {
  const res = await apiFetch(`/api/v1/labels/print-jobs/${encodeURIComponent(jobId)}/status`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status }),
  })
  if (!res.ok) {
    const msg = await safeReadText(res)
    throw new Error(msg || `Update status failed (${res.status})`)
  }
}

async function safeReadText(res: Response): Promise<string> {
  try {
    return await res.text()
  } catch {
    return ""
  }
}

