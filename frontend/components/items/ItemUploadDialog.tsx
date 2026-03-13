import { useMemo, useState } from "react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogFooter,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { BulkItemUploadResponse, uploadItemImage, uploadItems } from "@/lib/items"
import { useToast } from "@/components/ui/use-toast"
import { Download, Upload } from "lucide-react"

const MAX_IMAGE_UPLOAD_BYTES = 1_000_000
const RESIZE_MAX_DIMENSION_STEPS = [1024, 900, 800, 700, 600]
const JPEG_QUALITY_STEPS = [0.85, 0.78, 0.7, 0.62, 0.55, 0.5]

function stripFileExtension(name: string): string {
  const idx = name.lastIndexOf(".")
  return idx > 0 ? name.slice(0, idx) : name
}

function buildCompressedFileName(originalName: string): string {
  return `${stripFileExtension(originalName)}.jpg`
}

function loadImageFromFile(file: File): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      URL.revokeObjectURL(url)
      resolve(img)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error("Invalid image file"))
    }
    img.src = url
  })
}

function canvasToJpegBlob(canvas: HTMLCanvasElement, quality: number): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (!blob) {
          reject(new Error("Failed to compress image"))
          return
        }
        resolve(blob)
      },
      "image/jpeg",
      quality
    )
  })
}

async function maybeCompressImageForUpload(file: File): Promise<File> {
  if (!file.type.startsWith("image/")) return file
  if (file.size <= MAX_IMAGE_UPLOAD_BYTES) return file

  const img = await loadImageFromFile(file)

  for (const maxDim of RESIZE_MAX_DIMENSION_STEPS) {
    const scale = Math.min(1, maxDim / Math.max(img.width, img.height))
    const width = Math.max(1, Math.round(img.width * scale))
    const height = Math.max(1, Math.round(img.height * scale))

    const canvas = document.createElement("canvas")
    canvas.width = width
    canvas.height = height

    const ctx = canvas.getContext("2d")
    if (!ctx) {
      throw new Error("Failed to compress image")
    }

    ctx.fillStyle = "#fff"
    ctx.fillRect(0, 0, width, height)
    ctx.drawImage(img, 0, 0, width, height)

    for (const q of JPEG_QUALITY_STEPS) {
      const blob = await canvasToJpegBlob(canvas, q)
      if (blob.size <= MAX_IMAGE_UPLOAD_BYTES) {
        return new File([blob], buildCompressedFileName(file.name), {
          type: blob.type,
          lastModified: Date.now(),
        })
      }
    }
  }

  throw new Error(
    "Image is too large to upload. Please choose a smaller image."
  )
}

export function ItemUploadDialog({ onUpload }: { onUpload: () => void }) {
  const [open, setOpen] = useState(false)
  const [file, setFile] = useState<File | null>(null)
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState<BulkItemUploadResponse | null>(null)
  const [uploadingImages, setUploadingImages] = useState<Record<string, boolean>>(
    {}
  )
  const { toast } = useToast()

  function downloadTemplate() {
    const headers = [
      "Name",
      "Preferred Vendor",
      "UOM",
      "COGS",
      "UNIT price",
      "Image path or URL",
    ]
    const sample = [
      "Sample Item",
      "Sample Vendor",
      "pcs",
      "10.00",
      "15.00",
      "https://example.com/sample.jpg",
    ]

    const escape = (v: string) => `"${String(v).replaceAll('"', '""')}"`
    const lines = [headers.join(","), sample.map(escape).join(",")]
    const blob = new Blob([lines.join("\n")], { type: "text/csv;charset=utf-8" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = "bulk-item-upload-template.csv"
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  }

  async function handleUpload() {
    if (!file) return

    setLoading(true)
    try {
      const formData = new FormData()
      formData.append("file", file)
      const res = await uploadItems(formData)
      setResult(res)
      setFile(null)
      if (res.createdCount > 0) {
        onUpload()
      }
      toast({
        title: "Upload complete",
        description: `Created: ${res.createdCount}, Skipped: ${res.skippedCount}, Failed: ${res.failedCount}`,
      })
    } catch (err) {
      toast({
        variant: "destructive",
        title: "Error",
        description: err instanceof Error ? err.message : "Failed to upload",
      })
    } finally {
      setLoading(false)
    }
  }

  const missingImages = useMemo(() => result?.missingImages ?? [], [result])

  async function handleUploadImage(itemId: string, imageFile: File) {
    setUploadingImages((prev) => ({ ...prev, [itemId]: true }))
    try {
      const toUpload = await maybeCompressImageForUpload(imageFile)
      await uploadItemImage(itemId, toUpload)
      setResult((prev) => {
        if (!prev) return prev
        return {
          ...prev,
          missingImages: prev.missingImages.filter((x) => x.itemId !== itemId),
        }
      })
      onUpload()
      toast({ title: "Image uploaded" })
    } catch (err) {
      toast({
        variant: "destructive",
        title: "Image upload failed",
        description: err instanceof Error ? err.message : "Failed to upload image",
      })
    } finally {
      setUploadingImages((prev) => ({ ...prev, [itemId]: false }))
    }
  }

  const headerText =
    "Name, Preferred Vendor, UOM, COGS, UNIT price, Image path or URL"

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" className="gap-2">
          <Upload className="h-4 w-4" />
          Import
        </Button>
      </DialogTrigger>
      <DialogContent className="w-[calc(100vw-2rem)] sm:max-w-4xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Import Items (CSV)</DialogTitle>
          <DialogDescription>
            Use the template to ensure the column names match exactly.
          </DialogDescription>
        </DialogHeader>

        {result ? (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div className="rounded-xl border border-border p-3">
                <div className="text-muted-foreground">Total rows</div>
                <div className="text-lg font-semibold">{result.totalRows}</div>
              </div>
              <div className="rounded-xl border border-border p-3">
                <div className="text-muted-foreground">Created</div>
                <div className="text-lg font-semibold">{result.createdCount}</div>
              </div>
              <div className="rounded-xl border border-border p-3">
                <div className="text-muted-foreground">Skipped</div>
                <div className="text-lg font-semibold">{result.skippedCount}</div>
              </div>
              <div className="rounded-xl border border-border p-3">
                <div className="text-muted-foreground">Failed</div>
                <div className="text-lg font-semibold">{result.failedCount}</div>
              </div>
            </div>

            {result.details.length === 0 ? (
              <div className="text-sm text-muted-foreground">
                No warnings or errors.
              </div>
            ) : (
              <div className="rounded-xl border border-border overflow-hidden">
                <div className="max-h-[320px] overflow-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="w-[90px]">Row</TableHead>
                        <TableHead className="w-[170px]">Status</TableHead>
                        <TableHead>Message</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {result.details.map((d) => (
                        <TableRow key={`${d.rowNumber}-${d.status}-${d.message}`}>
                          <TableCell className="font-mono text-xs">
                            {d.rowNumber}
                          </TableCell>
                          <TableCell>
                            <Badge
                              variant={
                                d.status === "FAILED"
                                  ? "destructive"
                                  : d.status === "SKIPPED"
                                    ? "outline"
                                    : "default"
                              }
                            >
                              {d.status}
                            </Badge>
                          </TableCell>
                          <TableCell className="whitespace-pre-wrap break-words">
                            {d.message}
                            {d.warning ? (
                              <div className="mt-1 text-xs text-muted-foreground">
                                {d.warning}
                              </div>
                            ) : null}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              </div>
            )}

            {missingImages.length > 0 ? (
              <div className="space-y-2">
                <div className="text-sm font-medium">
                  Upload images for created items
                </div>
                <div className="rounded-xl border border-border overflow-hidden">
                  <div className="max-h-[320px] overflow-auto">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead className="w-[220px]">Item</TableHead>
                          <TableHead className="w-[220px]">Vendor</TableHead>
                          <TableHead>Image</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {missingImages.map((m) => (
                          <TableRow key={m.itemId}>
                            <TableCell className="whitespace-pre-wrap break-words">
                              <div className="text-sm font-medium">{m.name}</div>
                              <div className="text-xs text-muted-foreground">
                                {m.code}
                              </div>
                            </TableCell>
                            <TableCell className="whitespace-pre-wrap break-words">
                              {m.vendorName ?? "-"}
                            </TableCell>
                            <TableCell>
                              <Input
                                type="file"
                                accept="image/*"
                                disabled={!!uploadingImages[m.itemId]}
                                onChange={(e) => {
                                  const f = e.target.files?.[0]
                                  e.target.value = ""
                                  if (!f) return
                                  handleUploadImage(m.itemId, f)
                                }}
                              />
                              {uploadingImages[m.itemId] ? (
                                <div className="mt-1 text-xs text-muted-foreground">
                                  Uploading...
                                </div>
                              ) : null}
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                </div>
              </div>
            ) : null}

            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => {
                  setResult(null)
                  setFile(null)
                  setUploadingImages({})
                }}
              >
                Upload another file
              </Button>
              <Button
                onClick={() => {
                  setResult(null)
                  setFile(null)
                  setUploadingImages({})
                  setOpen(false)
                }}
              >
                Close
              </Button>
            </DialogFooter>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center gap-2">
              <Button
                type="button"
                variant="outline"
                className="gap-2"
                onClick={downloadTemplate}
              >
                <Download className="h-4 w-4" />
                Download template
              </Button>
              <div className="text-sm text-muted-foreground">{headerText}</div>
            </div>

            <Input
              type="file"
              accept=".csv"
              onChange={(e) => setFile(e.target.files?.[0] || null)}
            />

            <DialogFooter>
              <Button onClick={handleUpload} disabled={!file || loading}>
                {loading ? "Uploading..." : "Upload"}
              </Button>
            </DialogFooter>
          </div>
        )}
      </DialogContent>
    </Dialog>
  )
}
