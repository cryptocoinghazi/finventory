"use client"

import { useFieldArray, useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { useMemo } from "react"
import { Button } from "@/components/ui/button"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Card, CardContent } from "@/components/ui/card"
import { Trash2, Plus } from "lucide-react"
import { Item } from "@/lib/items"
import { Party } from "@/lib/parties"
import { Warehouse } from "@/lib/warehouses"
import { PurchaseInvoiceInput } from "@/lib/purchase-invoices"

const invoiceSchema = z.object({
  invoiceDate: z.string().min(1, "Date is required"),
  partyId: z.string().min(1, "Vendor is required"),
  warehouseId: z.string().min(1, "Warehouse is required"),
  vendorInvoiceNumber: z.string().optional(),
  lines: z
    .array(
      z.object({
        itemId: z.string().min(1, "Item is required"),
        quantity: z.coerce.number().min(0.01, "Quantity must be > 0"),
        unitPrice: z.coerce.number().min(0, "Price must be >= 0"),
      })
    )
    .min(1, "At least one item is required"),
})

type InvoiceFormValues = z.infer<typeof invoiceSchema>

interface PurchaseInvoiceFormProps {
  initialData?: PurchaseInvoiceInput
  items: Item[]
  parties: Party[]
  warehouses: Warehouse[]
  onSubmit: (data: PurchaseInvoiceInput) => Promise<void>
  submitLabel?: string
}

export function PurchaseInvoiceForm({
  initialData,
  items,
  parties,
  warehouses,
  onSubmit,
  submitLabel = "Save Invoice",
}: PurchaseInvoiceFormProps) {
  const defaultValues: Partial<InvoiceFormValues> = {
    invoiceDate: initialData?.invoiceDate || new Date().toISOString().slice(0, 10),
    partyId: initialData?.partyId || "",
    warehouseId: initialData?.warehouseId || "",
    vendorInvoiceNumber: initialData?.vendorInvoiceNumber || "",
    lines: initialData?.lines?.map((l) => ({
      itemId: l.itemId,
      quantity: l.quantity,
      unitPrice: l.unitPrice,
    })) || [{ itemId: "", quantity: 1, unitPrice: 0 }],
  }

  const form = useForm<InvoiceFormValues>({
    resolver: zodResolver(invoiceSchema),
    defaultValues,
  })

  const { fields, append, remove } = useFieldArray({
    control: form.control,
    name: "lines",
  })

  const watchLines = form.watch("lines")

  // Calculate totals for preview
  const totals = useMemo(() => {
    let totalTaxable = 0
    let totalTax = 0
    let grandTotal = 0

    watchLines.forEach((line) => {
      const item = items.find((i) => i.id === line.itemId)
      const qty = Number(line.quantity) || 0
      const price = Number(line.unitPrice) || 0
      const taxable = qty * price
      const taxRate = item?.taxRate || 0
      const tax = (taxable * taxRate) / 100

      totalTaxable += taxable
      totalTax += tax
      grandTotal += taxable + tax
    })

    return { totalTaxable, totalTax, grandTotal }
  }, [watchLines, items])

  // Handle item selection change to auto-fill price
  const handleItemChange = (index: number, itemId: string) => {
    const item = items.find((i) => i.id === itemId)
    if (item) {
      form.setValue(`lines.${index}.unitPrice`, item.unitPrice)
      form.setValue(`lines.${index}.itemId`, itemId) // Trigger re-render/calc
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8">
        <div className="grid gap-4 md:grid-cols-4">
          <FormField
            control={form.control}
            name="invoiceDate"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Invoice Date</FormLabel>
                <FormControl>
                  <Input type="date" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="partyId"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Vendor</FormLabel>
                <Select onValueChange={field.onChange} defaultValue={field.value}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Select vendor" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {parties
                      .filter((p) => p.type === "VENDOR")
                      .map((p) => (
                        <SelectItem key={p.id} value={p.id}>
                          {p.name}
                        </SelectItem>
                      ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="warehouseId"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Warehouse</FormLabel>
                <Select onValueChange={field.onChange} defaultValue={field.value}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Select warehouse" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {warehouses.map((w) => (
                      <SelectItem key={w.id} value={w.id}>
                        {w.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="vendorInvoiceNumber"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Vendor Invoice #</FormLabel>
                <FormControl>
                  <Input placeholder="Optional" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>

        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-medium">Items</h3>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => append({ itemId: "", quantity: 1, unitPrice: 0 })}
            >
              <Plus className="mr-2 h-4 w-4" /> Add Item
            </Button>
          </div>

          <div className="rounded-md border">
            <div className="grid grid-cols-12 gap-4 p-4 font-medium text-muted-foreground border-b bg-muted/50">
              <div className="col-span-5">Item</div>
              <div className="col-span-2 text-right">Qty</div>
              <div className="col-span-2 text-right">Price</div>
              <div className="col-span-2 text-right">Total</div>
              <div className="col-span-1"></div>
            </div>
            
            {fields.map((field, index) => {
               // Calculate line total for display
               const currentLine = watchLines[index]
               const qty = Number(currentLine?.quantity) || 0
               const price = Number(currentLine?.unitPrice) || 0
               const item = items.find(i => i.id === currentLine?.itemId)
               const tax = (qty * price * (item?.taxRate || 0)) / 100
               const lineTotal = (qty * price) + tax

              return (
                <div key={field.id} className="grid grid-cols-12 gap-4 p-4 items-center border-b last:border-0">
                  <div className="col-span-5">
                    <FormField
                      control={form.control}
                      name={`lines.${index}.itemId`}
                      render={({ field }) => (
                        <FormItem>
                          <Select
                            onValueChange={(val) => {
                              field.onChange(val)
                              handleItemChange(index, val)
                            }}
                            defaultValue={field.value}
                          >
                            <FormControl>
                              <SelectTrigger>
                                <SelectValue placeholder="Select item" />
                              </SelectTrigger>
                            </FormControl>
                            <SelectContent>
                              {items.map((item) => (
                                <SelectItem key={item.id} value={item.id}>
                                  {item.code} - {item.name}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>

                  <div className="col-span-2">
                    <FormField
                      control={form.control}
                      name={`lines.${index}.quantity`}
                      render={({ field }) => (
                        <FormItem>
                          <FormControl>
                            <Input 
                              type="number" 
                              min="0.01" 
                              step="0.01" 
                              {...field} 
                              className="text-right"
                            />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>

                  <div className="col-span-2">
                    <FormField
                      control={form.control}
                      name={`lines.${index}.unitPrice`}
                      render={({ field }) => (
                        <FormItem>
                          <FormControl>
                            <Input 
                              type="number" 
                              min="0" 
                              step="0.01" 
                              {...field} 
                              className="text-right"
                            />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>

                  <div className="col-span-2 text-right font-medium">
                    {lineTotal.toFixed(2)}
                  </div>

                  <div className="col-span-1 text-right">
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      onClick={() => remove(index)}
                      disabled={fields.length === 1}
                    >
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        <div className="flex justify-end">
          <Card className="w-80">
            <CardContent className="pt-6 space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Taxable Amount:</span>
                <span>{totals.totalTaxable.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">Tax Amount:</span>
                <span>{totals.totalTax.toFixed(2)}</span>
              </div>
              <div className="flex justify-between font-bold text-lg pt-2 border-t">
                <span>Grand Total:</span>
                <span>{totals.grandTotal.toFixed(2)}</span>
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="flex justify-end gap-4">
          <Button type="submit" size="lg" disabled={form.formState.isSubmitting}>
            {form.formState.isSubmitting ? "Saving..." : submitLabel}
          </Button>
        </div>
      </form>
    </Form>
  )
}
