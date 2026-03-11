"use client"

import { useEffect, useMemo, useRef, useState } from "react"
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"
import { Item } from "@/lib/items"
import {
  LabelBarcodeFormat,
  LabelPrintPrepareResponse,
  LabelTemplateName,
  prepareLabelPrintJob,
  updateLabelPrintJobStatus,
} from "@/lib/labels"
import { getOrganizationProfile } from "@/lib/settings"

type Line = {
  item: Item
  quantity: number
}

type PrintPaper = "ROLL" | "A4"

const A4_WIDTH_MM = 210
const A4_HEIGHT_MM = 297
const A4_MARGIN_MM = 10
const A4_GAP_MM = 2

const LABEL_TEMPLATES: Record<
  LabelTemplateName,
  { title: string; widthIn: number; heightIn: number }
> = {
  LABEL_2X1: { title: "2″ × 1″", widthIn: 2, heightIn: 1 },
  LABEL_2X2: { title: "2″ × 2″", widthIn: 2, heightIn: 2 },
  LABEL_4X2: { title: "4″ × 2″", widthIn: 4, heightIn: 2 },
}

const SVG_NS = "http://www.w3.org/2000/svg"

function clampInt(value: string, fallback: number) {
  const n = Number.parseInt(value, 10)
  if (!Number.isFinite(n) || n < 1) return fallback
  return n
}

function escapeHtml(value: string) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;")
}

function computeTotal(lines: Line[]) {
  return lines.reduce((sum, l) => sum + (Number.isFinite(l.quantity) ? l.quantity : 0), 0)
}

function isValidEan13(value: string) {
  const v = value.trim()
  if (v.length !== 13) return false
  for (let i = 0; i < v.length; i++) {
    const c = v.charCodeAt(i)
    if (c < 48 || c > 57) return false
  }
  let sum = 0
  for (let i = 0; i < 12; i++) {
    const digit = v.charCodeAt(i) - 48
    sum += i % 2 === 0 ? digit : digit * 3
  }
  const check = (10 - (sum % 10)) % 10
  return check === v.charCodeAt(12) - 48
}

function buildLabelPages(prepared: LabelPrintPrepareResponse) {
  const pages: Array<{
    itemId: string
    name: string
    code: string
    barcode: string
    unitPrice: number
    format: LabelBarcodeFormat
  }> = []

  prepared.items.forEach((it) => {
    const qty = Math.max(0, Number(it.quantity) || 0)
    for (let i = 0; i < qty; i++) {
      pages.push({
        itemId: it.itemId,
        name: it.name,
        code: it.code,
        barcode: it.barcode,
        unitPrice: it.unitPrice,
        format: it.effectiveBarcodeFormat,
      })
    }
  })

  return pages
}

let jsBarcodeDefault: Promise<unknown> | null = null
async function getJsBarcodeDefault(): Promise<unknown> {
  if (!jsBarcodeDefault) {
    jsBarcodeDefault = import("jsbarcode").then((m) => m.default)
  }
  return jsBarcodeDefault
}

async function buildBarcodeSvgMarkup(opts: { value: string; format: LabelBarcodeFormat; height: number }) {
  const JsBarcode = (await getJsBarcodeDefault()) as unknown as (
    el: SVGSVGElement,
    value: string,
    options: Record<string, unknown>
  ) => void
  const svg = document.createElementNS(SVG_NS, "svg") as unknown as SVGSVGElement
  svg.setAttribute("shape-rendering", "crispEdges")

  const effective =
    opts.format === "EAN13" && isValidEan13(opts.value) ? ("EAN13" as const) : ("CODE128" as const)
  try {
    JsBarcode(svg, opts.value, {
      format: effective,
      displayValue: false,
      margin: 0,
      height: opts.height,
      width: 1.2,
    })
  } catch {
    try {
      JsBarcode(svg, opts.value, {
        format: "CODE128",
        displayValue: false,
        margin: 0,
        height: opts.height,
        width: 1.2,
      })
    } catch {}
  }
  return svg.outerHTML
}

function getOrCreatePrintIframe() {
  const existing = document.getElementById("label-print-iframe") as HTMLIFrameElement | null
  if (existing) return existing
  const iframe = document.createElement("iframe")
  iframe.id = "label-print-iframe"
  iframe.setAttribute("aria-hidden", "true")
  iframe.style.position = "fixed"
  iframe.style.right = "0"
  iframe.style.bottom = "0"
  iframe.style.width = "0"
  iframe.style.height = "0"
  iframe.style.border = "0"
  iframe.style.opacity = "0"
  iframe.style.pointerEvents = "none"
  document.body.appendChild(iframe)
  return iframe
}

function BarcodeSvg({
  value,
  format,
  height,
}: {
  value: string
  format: LabelBarcodeFormat
  height: number
}) {
  const svgRef = useRef<SVGSVGElement | null>(null)

  useEffect(() => {
    let cancelled = false
    async function render() {
      const el = svgRef.current
      if (!el) return
      const JsBarcode = (await import("jsbarcode")).default
      if (cancelled) return
      const effective =
        format === "EAN13" && isValidEan13(value) ? ("EAN13" as const) : ("CODE128" as const)
      try {
        JsBarcode(el, value, {
          format: effective,
          displayValue: false,
          margin: 0,
          height,
          width: 1.2,
        })
      } catch {
        try {
          JsBarcode(el, value, {
            format: "CODE128",
            displayValue: false,
            margin: 0,
            height,
            width: 1.2,
          })
        } catch {}
      }
    }
    render()
    return () => {
      cancelled = true
    }
  }, [format, height, value])

  return <svg ref={svgRef} className="w-full" />
}

function LabelPage({
  storeName,
  name,
  code,
  barcode,
  unitPrice,
  format,
  includeItemCode,
  templateName,
}: {
  storeName: string
  name: string
  code: string
  barcode: string
  unitPrice: number
  format: LabelBarcodeFormat
  includeItemCode: boolean
  templateName: LabelTemplateName
}) {
  const tmpl = LABEL_TEMPLATES[templateName]
  const barcodeHeight = tmpl.heightIn >= 2 ? 44 : 34
  return (
    <div
      className="label-page box-border flex flex-col justify-between overflow-hidden border border-black bg-white text-black"
      style={{ width: `${tmpl.widthIn}in`, height: `${tmpl.heightIn}in` }}
    >
      <div className="px-[0.06in] pt-[0.05in] text-[9.5px] leading-tight">
        {storeName ? <div className="truncate text-[8px] font-semibold">{storeName}</div> : null}
        <div className="truncate font-medium">{name}</div>
        {includeItemCode ? <div className="truncate text-[9px]">{code}</div> : null}
      </div>

      <div className="px-[0.06in]">
        <BarcodeSvg value={barcode} format={format} height={barcodeHeight} />
      </div>

      <div className="flex items-center justify-between px-[0.06in] pb-[0.05in] text-[10px]">
        <div className="font-semibold">₹{Number(unitPrice ?? 0).toLocaleString("en-IN")}</div>
        <div className="text-[8.5px]">{barcode}</div>
      </div>
    </div>
  )
}

export function LabelPrintDialog({
  open,
  onOpenChange,
  selectedItems,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  selectedItems: Item[]
}) {
  const [lines, setLines] = useState<Line[]>([])
  const [templateName, setTemplateName] = useState<LabelTemplateName>("LABEL_2X1")
  const [barcodeFormat, setBarcodeFormat] = useState<LabelBarcodeFormat>("AUTO")
  const [includeItemCode, setIncludeItemCode] = useState(false)
  const [printPaper, setPrintPaper] = useState<PrintPaper>("ROLL")
  const [a4Columns, setA4Columns] = useState(4)
  const [preparing, setPreparing] = useState(false)
  const [prepared, setPrepared] = useState<LabelPrintPrepareResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [statusSyncMessage, setStatusSyncMessage] = useState<string | null>(null)
  const [zoom, setZoom] = useState(1)
  const [orgName, setOrgName] = useState("")

  const totalLabels = useMemo(() => computeTotal(lines), [lines])

  useEffect(() => {
    if (!open) return
    const next: Line[] = selectedItems.map((it) => ({ item: it, quantity: 1 }))
    setLines(next)
    setTemplateName("LABEL_2X1")
    setBarcodeFormat("AUTO")
    setIncludeItemCode(false)
    setPrintPaper("ROLL")
    setA4Columns(4)
    setPrepared(null)
    setError(null)
    setStatusSyncMessage(null)
    setZoom(1)

    getOrganizationProfile()
      .then((p) => setOrgName((p.companyName || "").trim()))
      .catch(() => {})
  }, [open, selectedItems])

  useEffect(() => {
    if (open) return
    if (prepared?.jobId && prepared.status !== "PRINTED") {
      updateLabelPrintJobStatus(prepared.jobId, "CANCELLED").catch(() => {})
      setPrepared((p) => (p ? { ...p, status: "CANCELLED" } : p))
    }
  }, [open, prepared?.jobId, prepared?.status])

  const previewPages = useMemo(() => (prepared ? buildLabelPages(prepared) : []), [prepared])
  const template = LABEL_TEMPLATES[templateName]
  const labelWidthMm = template.widthIn * 25.4
  const labelHeightMm = template.heightIn * 25.4
  const a4UsableWidthMm = A4_WIDTH_MM - A4_MARGIN_MM * 2
  const a4UsableHeightMm = A4_HEIGHT_MM - A4_MARGIN_MM * 2
  const a4MaxColumns = Math.max(
    1,
    Math.floor((a4UsableWidthMm + A4_GAP_MM) / (labelWidthMm + A4_GAP_MM))
  )
  const effectiveA4Columns = Math.min(Math.max(1, a4Columns), a4MaxColumns)
  const a4Rows = Math.max(
    1,
    Math.floor((a4UsableHeightMm + A4_GAP_MM) / (labelHeightMm + A4_GAP_MM))
  )
  const a4PerPage = effectiveA4Columns * a4Rows

  const previewLimit = Math.min(200, previewPages.length)
  const previewLabels = useMemo(
    () => previewPages.slice(0, previewLimit),
    [previewLimit, previewPages]
  )
  const previewSheets = useMemo(() => {
    if (printPaper !== "A4") return []
    const chunks: typeof previewLabels[] = []
    const size = Math.max(1, a4PerPage)
    for (let i = 0; i < previewLabels.length; i += size) {
      chunks.push(previewLabels.slice(i, i + size))
    }
    return chunks
  }, [a4PerPage, previewLabels, printPaper])

  const totalPrintableLabels = previewPages.length
  const totalPages =
    printPaper === "A4"
      ? Math.max(1, Math.ceil(totalPrintableLabels / Math.max(1, a4PerPage)))
      : totalPrintableLabels

  async function resolveStoreName() {
    const existing = (orgName || "").trim()
    if (existing) return existing
    try {
      const p = await getOrganizationProfile()
      const next = (p.companyName || "").trim()
      if (next) setOrgName(next)
      return next
    } catch {
      return ""
    }
  }

  async function printPreparedInIframe() {
    if (!prepared) return

    const tmpl = LABEL_TEMPLATES[templateName]
    const barcodeHeight = tmpl.heightIn >= 2 ? 44 : 34
    const all = buildLabelPages(prepared)

    const barcodeCache = new Map<string, string>()
    async function barcodeSvgFor(value: string, format: LabelBarcodeFormat) {
      const key = `${format}|${barcodeHeight}|${value}`
      const existing = barcodeCache.get(key)
      if (existing) return existing
      const svg = await buildBarcodeSvgMarkup({ value, format, height: barcodeHeight })
      barcodeCache.set(key, svg)
      return svg
    }

    const includeCode = prepared.includeItemCode
    const storeName = await resolveStoreName()
    const safeStore = escapeHtml(storeName)

    const labelCss = `
      * { box-sizing: border-box; }
      html, body { margin: 0; padding: 0; background: white; color: black; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
      .label { border: 1px solid #000; background: #fff; overflow: hidden; display: flex; flex-direction: column; justify-content: space-between; }
      .top { padding: 0.05in 0.06in 0; font-size: 9.5px; line-height: 1.1; }
      .store { font-size: 8px; font-weight: 600; }
      .name { font-weight: 600; }
      .code { font-size: 9px; }
      .barcode { padding: 0 0.06in; }
      .barcode svg { width: 100%; height: auto; shape-rendering: crispEdges; }
      .bottom { display: flex; align-items: center; justify-content: space-between; padding: 0 0.06in 0.05in; font-size: 10px; }
      .barcodeText { font-size: 8.5px; }
    `

    function labelHtml(opts: {
      name: string
      code: string
      barcode: string
      unitPrice: number
      barcodeSvg: string
    }) {
      const name = escapeHtml(opts.name)
      const code = escapeHtml(opts.code)
      const barcode = escapeHtml(opts.barcode)
      const price = `₹${Number(opts.unitPrice ?? 0).toLocaleString("en-IN")}`
      return `
        <div class="label" style="width:${tmpl.widthIn}in; height:${tmpl.heightIn}in;">
          <div class="top">
            ${safeStore ? `<div class="store">${safeStore}</div>` : ""}
            <div class="name">${name}</div>
            ${includeCode ? `<div class="code">${code}</div>` : ""}
          </div>
          <div class="barcode">${opts.barcodeSvg}</div>
          <div class="bottom">
            <div class="price"><b>${escapeHtml(price)}</b></div>
            <div class="barcodeText">${barcode}</div>
          </div>
        </div>
      `
    }

    let bodyHtml = ""
    let pageCss = ""

    if (printPaper === "A4") {
      const cols = effectiveA4Columns
      const sheetWidthMm = A4_WIDTH_MM - A4_MARGIN_MM * 2
      const sheetHeightMm = A4_HEIGHT_MM - A4_MARGIN_MM * 2
      const perPage = Math.max(1, a4PerPage)
      const sheets: typeof all[] = []
      for (let i = 0; i < all.length; i += perPage) {
        sheets.push(all.slice(i, i + perPage))
      }

      const renderedSheets = await Promise.all(
        sheets.map(async (sheet) => {
          const labels = await Promise.all(
            sheet.map(async (p) =>
              labelHtml({
                name: p.name,
                code: p.code,
                barcode: p.barcode,
                unitPrice: p.unitPrice,
                barcodeSvg: await barcodeSvgFor(p.barcode, p.format),
              })
            )
          )
          return `
            <div class="sheet">
              ${labels.join("")}
            </div>
          `
        })
      )

      bodyHtml = renderedSheets.join("")
      pageCss = `
        @page { size: A4; margin: ${A4_MARGIN_MM}mm; }
        .sheet {
          width: ${sheetWidthMm}mm;
          height: ${sheetHeightMm}mm;
          display: grid;
          grid-template-columns: repeat(${cols}, ${tmpl.widthIn}in);
          grid-auto-rows: ${tmpl.heightIn}in;
          gap: ${A4_GAP_MM}mm;
          justify-content: center;
          align-content: start;
          page-break-after: always;
          break-after: page;
        }
        .sheet:last-child { page-break-after: auto; break-after: auto; }
      `
    } else {
      const rendered = await Promise.all(
        all.map(async (p) =>
          labelHtml({
            name: p.name,
            code: p.code,
            barcode: p.barcode,
            unitPrice: p.unitPrice,
            barcodeSvg: await barcodeSvgFor(p.barcode, p.format),
          })
        )
      )
      bodyHtml = rendered
        .map((h, i) => `<div class="rollPage"${i === rendered.length - 1 ? ` data-last="1"` : ""}>${h}</div>`)
        .join("")
      pageCss = `
        @page { size: ${tmpl.widthIn}in ${tmpl.heightIn}in; margin: 0; }
        .rollPage { page-break-after: always; break-after: page; }
        .rollPage[data-last="1"] { page-break-after: auto; break-after: auto; }
      `
    }

    const html = `<!doctype html>
      <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1" />
          <title>Label Print</title>
          <style>
            ${labelCss}
            ${pageCss}
          </style>
        </head>
        <body>
          ${bodyHtml}
        </body>
      </html>
    `

    const iframe = getOrCreatePrintIframe()
    const win = iframe.contentWindow
    const doc = iframe.contentDocument
    if (!win || !doc) throw new Error("Print frame not available")

    await new Promise<void>((resolve) => {
      const cleanup = () => {
        win.removeEventListener("load", onLoad)
        resolve()
      }
      const onLoad = () => cleanup()
      win.addEventListener("load", onLoad)
      doc.open()
      doc.write(html)
      doc.close()
      setTimeout(() => cleanup(), 30)
    })

    await new Promise<void>((resolve) => {
      const onAfterPrint = () => resolve()
      win.addEventListener("afterprint", onAfterPrint, { once: true })
      win.focus()
      win.print()
    })
  }

  async function onPrepare() {
    setError(null)
    setStatusSyncMessage(null)
    setPreparing(true)
    setPrepared(null)
    try {
      const res = await prepareLabelPrintJob({
        items: lines.map((l) => ({ itemId: l.item.id, quantity: l.quantity })),
        templateName,
        barcodeFormat,
        includeItemCode,
      })
      setPrepared(res)
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to prepare labels")
    } finally {
      setPreparing(false)
    }
  }

  async function onPrint() {
    if (!prepared?.jobId) return
    setError(null)
    setStatusSyncMessage(null)
    try {
      await updateLabelPrintJobStatus(prepared.jobId, "PRINT_DIALOG_OPENED")
    } catch {}
    try {
      await printPreparedInIframe()
      setPrepared((p) => (p ? { ...p, status: "PRINTED" } : p))
    } catch (e) {
      setError(e instanceof Error ? e.message : "Print failed")
      return
    }
    try {
      await updateLabelPrintJobStatus(prepared.jobId, "PRINTED")
    } catch {
      setStatusSyncMessage("Print dialog closed, but status sync failed.")
    }
  }

  const invalidCount = prepared?.invalidItems?.length ?? 0
  const canPrint = Boolean(prepared?.jobId) && invalidCount === 0 && prepared!.items.length > 0

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="flex max-h-[90vh] max-w-5xl flex-col overflow-hidden">
          <DialogHeader>
            <DialogTitle>Print Labels</DialogTitle>
          </DialogHeader>

          <div className="grid flex-1 min-h-0 gap-5 overflow-hidden lg:grid-cols-2">
            <div className="flex min-h-0 flex-col gap-4">
              {error ? <div className="text-sm text-destructive">{error}</div> : null}
              {statusSyncMessage ? (
                <div className="text-sm text-muted-foreground">{statusSyncMessage}</div>
              ) : null}

              {prepared && invalidCount > 0 ? (
                <div className="rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm">
                  <div className="font-medium text-destructive">
                    Some items can’t be printed ({invalidCount})
                  </div>
                  <div className="mt-2 space-y-2">
                    {prepared.invalidItems.map((it) => (
                      <div key={it.itemId} className="flex items-start justify-between gap-3">
                        <div className="min-w-0">
                          <div className="truncate font-medium">
                            {it.name ?? it.code ?? it.itemId}
                          </div>
                          <div className="text-xs text-muted-foreground">
                            {it.errors.join(" • ")}
                          </div>
                        </div>
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            setLines((prev) => prev.filter((l) => l.item.id !== it.itemId))
                            setPrepared(null)
                          }}
                        >
                          Remove
                        </Button>
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}

              <div className="grid gap-3 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label>Template</Label>
                  <Select
                    value={templateName}
                    onValueChange={(v) => {
                      setTemplateName(v as LabelTemplateName)
                      setPrepared(null)
                    }}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select template" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="LABEL_2X1">2″ × 1″</SelectItem>
                      <SelectItem value="LABEL_2X2">2″ × 2″</SelectItem>
                      <SelectItem value="LABEL_4X2">4″ × 2″</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>Barcode Format</Label>
                  <Select
                    value={barcodeFormat}
                    onValueChange={(v) => {
                      setBarcodeFormat(v as LabelBarcodeFormat)
                      setPrepared(null)
                    }}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select barcode format" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="AUTO">Auto (EAN-13 if valid, else Code 128)</SelectItem>
                      <SelectItem value="CODE128">Code 128</SelectItem>
                      <SelectItem value="EAN13">EAN-13</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="grid gap-3 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label>Paper</Label>
                  <Select
                    value={printPaper}
                    onValueChange={(v) => {
                      setPrintPaper(v as PrintPaper)
                      setPrepared(null)
                    }}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select paper" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ROLL">Single label (roll)</SelectItem>
                      <SelectItem value="A4">A4 sheet</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label>Labels per row</Label>
                  <Select
                    value={String(effectiveA4Columns)}
                    onValueChange={(v) => {
                      setA4Columns(clampInt(v, 4))
                      setPrepared(null)
                    }}
                    disabled={printPaper !== "A4"}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Columns" />
                    </SelectTrigger>
                    <SelectContent>
                      {Array.from({ length: a4MaxColumns }, (_, i) => String(i + 1)).map((v) => (
                        <SelectItem key={v} value={v}>
                          {v}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              {printPaper === "A4" ? (
                <div className="text-xs text-muted-foreground">
                  A4 capacity: {effectiveA4Columns} × {a4Rows} = {a4PerPage} labels per page
                </div>
              ) : null}

              <div className="flex items-center justify-between rounded-lg border bg-card px-3 py-2">
                <div className="text-sm text-muted-foreground">Total labels</div>
                <div className="text-sm font-medium">{totalLabels}</div>
              </div>

              <Separator />

              <div className="flex min-h-0 flex-col gap-2">
                <div className="grid grid-cols-12 gap-2 text-xs text-muted-foreground">
                  <div className="col-span-6">Item</div>
                  <div className="col-span-3">Barcode</div>
                  <div className="col-span-2 text-right">Price</div>
                  <div className="col-span-1 text-right">Qty</div>
                </div>

                <div className="min-h-0 flex-1 space-y-2 overflow-auto pr-1">
                  {lines.map((l) => (
                    <div key={l.item.id} className="grid grid-cols-12 items-center gap-2 text-sm">
                      <div className="col-span-6 min-w-0">
                        <div className="truncate font-medium">{l.item.name}</div>
                        <div className="truncate text-xs text-muted-foreground">{l.item.code}</div>
                      </div>
                      <div className="col-span-3 truncate text-xs text-muted-foreground">
                        {l.item.barcode || "—"}
                      </div>
                      <div className="col-span-2 text-right">
                        ₹{Number(l.item.unitPrice ?? 0).toLocaleString("en-IN")}
                      </div>
                      <div className="col-span-1 flex justify-end">
                        <Input
                          inputMode="numeric"
                          className="h-8 w-16 text-right"
                          value={String(l.quantity)}
                          onChange={(e) => {
                            const nextQty = clampInt(e.target.value, 1)
                            setLines((prev) =>
                              prev.map((x) =>
                                x.item.id === l.item.id ? { ...x, quantity: nextQty } : x
                              )
                            )
                            setPrepared(null)
                          }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="flex items-center gap-2">
                <Button type="button" onClick={onPrepare} disabled={preparing || lines.length === 0}>
                  {preparing ? "Preparing..." : "Preview"}
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setIncludeItemCode((v) => !v)}
                >
                  {includeItemCode ? "Hide Code" : "Show Code"}
                </Button>
                <div className="ml-auto flex items-center gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setZoom((z) => Math.max(0.6, Math.round((z - 0.1) * 10) / 10))}
                    disabled={!prepared}
                  >
                    -
                  </Button>
                  <div className="w-14 text-center text-sm text-muted-foreground">
                    {Math.round(zoom * 100)}%
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setZoom((z) => Math.min(1.6, Math.round((z + 0.1) * 10) / 10))}
                    disabled={!prepared}
                  >
                    +
                  </Button>
                </div>
              </div>
            </div>

            <div className="flex min-h-0 flex-col gap-3">
              <div className="rounded-lg border bg-card p-3">
                <div className="text-sm font-medium">Preview</div>
                <div className="text-xs text-muted-foreground">
                  Template: {LABEL_TEMPLATES[templateName].title}
                  {printPaper === "A4" ? ` • A4 (${effectiveA4Columns}/row)` : ""}
                  {totalPrintableLabels ? ` • Pages: ${totalPages}` : ""}
                </div>
              </div>

              {!prepared ? (
                <div className="rounded-lg border bg-muted/30 p-6 text-center text-sm text-muted-foreground">
                  Click Preview to generate labels
                </div>
              ) : previewPages.length === 0 ? (
                <div className="rounded-lg border bg-muted/30 p-6 text-center text-sm text-muted-foreground">
                  No printable labels in this batch
                </div>
              ) : (
                <div className="min-h-0 flex-1 overflow-auto rounded-lg border bg-muted/10 p-3">
                  <div className="origin-top-left" style={{ transform: `scale(${zoom})` }}>
                    {printPaper === "A4" ? (
                      <div className="space-y-3">
                        {previewSheets.map((sheet, sheetIdx) => (
                          <div
                            key={`preview-sheet-${sheetIdx}`}
                            className="rounded border bg-white p-2"
                            style={{
                              width: `${A4_WIDTH_MM}mm`,
                            }}
                          >
                            <div
                              style={{
                                width: `${a4UsableWidthMm}mm`,
                                height: `${a4UsableHeightMm}mm`,
                                display: "grid",
                                gridTemplateColumns: `repeat(${effectiveA4Columns}, ${template.widthIn}in)`,
                                gridAutoRows: `${template.heightIn}in`,
                                gap: `${A4_GAP_MM}mm`,
                                justifyContent: "center",
                                alignContent: "start",
                                background: "white",
                              }}
                            >
                              {sheet.map((p, idx) => (
                                <LabelPage
                                  key={`${p.itemId}-${sheetIdx}-${idx}`}
                                  storeName={orgName}
                                  name={p.name}
                                  code={p.code}
                                  barcode={p.barcode}
                                  unitPrice={p.unitPrice}
                                  format={p.format}
                                  includeItemCode={prepared.includeItemCode}
                                  templateName={templateName}
                                />
                              ))}
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="flex flex-wrap gap-2">
                        {previewLabels.map((p, idx) => (
                          <div key={`${p.itemId}-${idx}`} className="rounded border bg-white p-0">
                            <LabelPage
                              storeName={orgName}
                              name={p.name}
                              code={p.code}
                              barcode={p.barcode}
                              unitPrice={p.unitPrice}
                              format={p.format}
                              includeItemCode={prepared.includeItemCode}
                              templateName={templateName}
                            />
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                  {previewPages.length > previewLimit ? (
                    <div className="mt-3 text-xs text-muted-foreground">
                      Showing {previewLimit} of {previewPages.length} labels in preview.
                    </div>
                  ) : null}
                </div>
              )}
            </div>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="button" onClick={onPrint} disabled={!canPrint}>
              Print
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  )
}
