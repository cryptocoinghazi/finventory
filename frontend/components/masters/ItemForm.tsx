"use client"

import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { useRouter } from "next/navigation"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { FormLayout } from "@/components/ui-kit/FormLayout"
import { FormSectionCard } from "@/components/ui-kit/FormSectionCard"
import { SmartSelect } from "@/components/ui-kit/SmartSelect"
import { ItemInput } from "@/lib/items"
import { TaxSlab } from "@/lib/tax-slabs"
import { Party } from "@/lib/parties"
import { useState } from "react"
import { API_BASE } from "@/lib/api"

const itemSchema = z.object({
  name: z.string().min(1, "Name is required"),
  code: z.string().min(1, "Code is required"),
  category: z.string().max(100, "Category too long").optional(),
  barcode: z.string().max(64, "Barcode too long").optional(),
  hsnCode: z.string().optional(),
  uom: z.string().min(1, "UOM is required"),
  unitPrice: z.coerce.number().min(0, "Price must be >= 0"),
  taxRate: z.coerce.number().min(0, "Tax rate must be >= 0"),
  vendorId: z.string().optional(),
})

type FormValues = z.infer<typeof itemSchema>

export function ItemForm({
  initialValue,
  onSubmit,
  submitLabel,
}: {
  initialValue?: ItemInput & { vendorName?: string | null }
  submitLabel: string
  onSubmit: (input: ItemInput, imageFile: File | null) => Promise<void>
}) {
  const router = useRouter()
  const [serverError, setServerError] = useState<string | null>(null)
  // We'll track the selected slab ID just for the UI of SmartSelect, if possible.
  // Since we don't have slab ID in initialValue, we start undefined.
  const [selectedSlabId, setSelectedSlabId] = useState<string | undefined>(undefined)
  const [selectedVendorId, setSelectedVendorId] = useState<string | undefined>(
    initialValue?.vendorId || undefined
  )
  const [initialVendorName] = useState<string | undefined>(
    initialValue?.vendorName || undefined
  )
  const [imageFile, setImageFile] = useState<File | null>(null)

  const form = useForm<FormValues>({
    resolver: zodResolver(itemSchema),
    defaultValues: {
      name: initialValue?.name ?? "",
      code: initialValue?.code ?? "",
      category: initialValue?.category ?? "",
      barcode: initialValue?.barcode ?? "",
      hsnCode: initialValue?.hsnCode ?? "",
      uom: initialValue?.uom ?? "",
      unitPrice: initialValue?.unitPrice ?? 0,
      taxRate: initialValue?.taxRate ?? 0,
      vendorId: initialValue?.vendorId ?? undefined,
    },
  })

  async function handleSubmit(data: FormValues) {
    setServerError(null)
    try {
      await onSubmit({
        ...data,
        category: (data.category ?? "").trim() || null,
        barcode: (data.barcode ?? "").trim(),
        hsnCode: data.hsnCode || null,
      }, imageFile)
    } catch (err) {
      setServerError(err instanceof Error ? err.message : "An error occurred")
    }
  }

  const existingImageUrl = initialValue?.imageUrl
    ? (initialValue.imageUrl.startsWith("http") ? initialValue.imageUrl : `${API_BASE}${initialValue.imageUrl}`)
    : null

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)}>
        <FormLayout
          stickyFooter={
            <>
              <Button type="button" variant="outline" onClick={() => router.back()}>
                Cancel
              </Button>
              <Button type="submit" disabled={form.formState.isSubmitting}>
                {form.formState.isSubmitting ? "Saving..." : submitLabel}
              </Button>
            </>
          }
        >
          {serverError && (
            <div className="bg-destructive/15 text-destructive px-4 py-2 rounded-md">
              {serverError}
            </div>
          )}

          <FormSectionCard title="Basic Details">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Name</FormLabel>
                    <FormControl>
                      <Input placeholder="Item Name" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="code"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Code</FormLabel>
                    <FormControl>
                      <Input placeholder="Item Code" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="barcode"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Barcode</FormLabel>
                    <FormControl>
                      <Input placeholder="Optional barcode" {...field} value={field.value || ""} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="category"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Category</FormLabel>
                    <FormControl>
                      <Input placeholder="Optional category" {...field} value={field.value || ""} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="hsnCode"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>HSN Code</FormLabel>
                    <FormControl>
                      <Input placeholder="HSN Code" {...field} value={field.value || ""} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="uom"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>UOM</FormLabel>
                    <FormControl>
                      <Input placeholder="e.g. PCS, KG" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="vendorId"
                render={() => (
                  <FormItem>
                    <FormLabel>Preferred Vendor</FormLabel>
                    <FormControl>
                      <SmartSelect<Party>
                        endpoint="/api/v1/parties?type=VENDOR"
                        labelKey="name"
                        valueKey="id"
                        value={selectedVendorId}
                        placeholder="Select Vendor..."
                        initialLabel={initialVendorName ?? undefined}
                        onSelect={(id) => {
                          setSelectedVendorId(id)
                          form.setValue("vendorId", id)
                        }}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
          </FormSectionCard>

          <FormSectionCard title="Image">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 items-start">
              <div className="space-y-2">
                <FormLabel>Item Image</FormLabel>
                <Input
                  type="file"
                  accept="image/*"
                  onChange={(e) => setImageFile(e.target.files?.[0] ?? null)}
                />
                {imageFile ? (
                  <div className="text-sm text-muted-foreground truncate">{imageFile.name}</div>
                ) : null}
              </div>
              <div className="space-y-2">
                <FormLabel>Current Image</FormLabel>
                {existingImageUrl ? (
                  <div className="h-24 w-24 rounded-md overflow-hidden border bg-muted">
                    <img src={existingImageUrl} alt="Item" className="h-full w-full object-cover" />
                  </div>
                ) : (
                  <div className="text-sm text-muted-foreground">No image uploaded.</div>
                )}
              </div>
            </div>
          </FormSectionCard>

          <FormSectionCard title="Pricing & Tax">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="unitPrice"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Unit Price (₹)</FormLabel>
                    <FormControl>
                      <Input type="number" step="0.01" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              
              <div className="flex flex-col gap-2">
                 <FormField
                  control={form.control}
                  name="taxRate"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Tax Rate (%)</FormLabel>
                      <div className="flex gap-2">
                        <FormControl>
                          <Input type="number" step="0.01" {...field} className="w-24" />
                        </FormControl>
                        <div className="flex-1">
                            <SmartSelect<TaxSlab>
                                endpoint="/api/v1/tax-slabs"
                                labelKey="description"
                                valueKey="id"
                                value={selectedSlabId}
                                placeholder="Select Tax Slab..."
                                onSelect={(id, slab) => {
                                    setSelectedSlabId(id)
                                    if (slab) {
                                        form.setValue("taxRate", slab.rate)
                                    }
                                }}
                                renderOption={(slab) => {
                                    return <span>{slab.description} ({slab.rate}%)</span>
                                }}
                            />
                        </div>
                      </div>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
            </div>
          </FormSectionCard>
        </FormLayout>
      </form>
    </Form>
  )
}
