
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
import { SalesReturnInput } from "@/lib/sales-returns"
import { SalesInvoice } from "@/lib/sales-invoices"

const returnSchema = z.object({
  returnDate: z.string().min(1, "Date is required"),
  salesInvoiceId: z.string().optional(),
  partyId: z.string().min(1, "Customer is required"),
  warehouseId: z.string().min(1, "Warehouse is required"),
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

type ReturnFormValues = z.infer<typeof returnSchema>

interface SalesReturnFormProps {
  initialData?: SalesReturnInput
  items: Item[]
  parties: Party[]
  warehouses: Warehouse[]
  invoices?: SalesInvoice[]
  onSubmit: (data: SalesReturnInput) => Promise<void>
  submitLabel?: string
}

export function SalesReturnForm({
  initialData,
  items,
  parties,
  warehouses,
  invoices = [],
  onSubmit,
  submitLabel = "Create Return",
}: SalesReturnFormProps) {
  const defaultValues: Partial<ReturnFormValues> = {
    returnDate: initialData?.returnDate || new Date().toISOString().slice(0, 10),
    salesInvoiceId: initialData?.salesInvoiceId || "none",
    partyId: initialData?.partyId || "",
    warehouseId: initialData?.warehouseId || "",
    lines: initialData?.lines?.map((l) => ({
      itemId: l.itemId,
      quantity: l.quantity,
      unitPrice: l.unitPrice,
    })) || [{ itemId: "", quantity: 1, unitPrice: 0 }],
  }

  const form = useForm<ReturnFormValues>({
    resolver: zodResolver(returnSchema),
    defaultValues,
  })

  const { fields, append, remove, replace } = useFieldArray({
    control: form.control,
    name: "lines",
  })

  const watchLines = form.watch("lines")

  const totals = useMemo(() => {
    let totalTaxable = 0
    let totalTax = 0
    let grandTotal = 0

    watchLines?.forEach((line) => {
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

  const handleItemChange = (index: number, itemId: string) => {
    const item = items.find((i) => i.id === itemId)
    if (item) {
      form.setValue(`lines.${index}.unitPrice`, item.unitPrice)
      form.setValue(`lines.${index}.itemId`, itemId)
    }
  }

  const handleInvoiceChange = (invoiceId: string) => {
    if (!invoiceId || invoiceId === "none") {
        form.setValue("salesInvoiceId", undefined)
        return
    }
    
    const invoice = invoices.find((i) => i.id === invoiceId)
    if (invoice) {
      form.setValue("partyId", invoice.partyId)
      form.setValue("warehouseId", invoice.warehouseId)
      form.setValue("salesInvoiceId", invoiceId)

      const newLines = invoice.lines.map((line) => ({
        itemId: line.itemId,
        quantity: line.quantity,
        unitPrice: line.unitPrice,
      }))
      replace(newLines)
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit((data) => onSubmit({ ...data, returnNumber: "", salesInvoiceId: data.salesInvoiceId === "none" ? undefined : data.salesInvoiceId }))} className="space-y-8">
        <div className="grid gap-4 md:grid-cols-3">
          <FormField
            control={form.control}
            name="returnDate"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Return Date</FormLabel>
                <FormControl>
                  <Input type="date" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

        <FormField
            control={form.control}
            name="salesInvoiceId"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Link Invoice (Optional)</FormLabel>
                <Select
                  onValueChange={(val) => handleInvoiceChange(val)}
                  defaultValue={field.value || "none"}
                >
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Select invoice" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value="none">None</SelectItem>
                    {invoices.map((inv) => (
                      <SelectItem key={inv.id} value={inv.id}>
                        {inv.invoiceNumber} ({inv.invoiceDate})
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
            name="partyId"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Customer</FormLabel>
                <Select onValueChange={field.onChange} value={field.value}>
                  <FormControl>
                    <SelectTrigger>
                      <SelectValue placeholder="Select customer" />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {parties
                      .filter((p) => p.type === "CUSTOMER")
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
                <Select onValueChange={field.onChange} value={field.value}>
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

          <div className="border rounded-md divide-y">
            {fields.map((field, index) => (
              <div key={field.id} className="p-4 grid gap-4 md:grid-cols-12 items-end">
                <div className="md:col-span-5">
                  <FormField
                    control={form.control}
                    name={`lines.${index}.itemId`}
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Item</FormLabel>
                        <Select
                          onValueChange={(val) => handleItemChange(index, val)}
                          value={field.value}
                        >
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder="Select item" />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            {items.map((item) => (
                              <SelectItem key={item.id} value={item.id}>
                                {item.name} ({item.code})
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>

                <div className="md:col-span-2">
                  <FormField
                    control={form.control}
                    name={`lines.${index}.quantity`}
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Quantity</FormLabel>
                        <FormControl>
                          <Input type="number" step="0.01" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>

                <div className="md:col-span-3">
                  <FormField
                    control={form.control}
                    name={`lines.${index}.unitPrice`}
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Unit Price</FormLabel>
                        <FormControl>
                          <Input type="number" step="0.01" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                </div>

                <div className="md:col-span-2 flex justify-end">
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="text-destructive"
                    onClick={() => remove(index)}
                    disabled={fields.length === 1}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            ))}
          </div>

          <Card>
            <CardContent className="pt-6">
              <div className="flex justify-end space-y-2">
                <div className="w-64 space-y-2">
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Taxable:</span>
                    <span>₹{totals.totalTaxable.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-muted-foreground">Tax:</span>
                    <span>₹{totals.totalTax.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between font-bold text-lg border-t pt-2">
                    <span>Total:</span>
                    <span>₹{totals.grandTotal.toFixed(2)}</span>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="flex justify-end gap-4">
          <Button type="submit">{submitLabel}</Button>
        </div>
      </form>
    </Form>
  )
}
