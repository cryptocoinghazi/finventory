"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { FormLayout } from "@/components/ui-kit/FormLayout"
import { FormSectionCard } from "@/components/ui-kit/FormSectionCard"
import { SmartSelect } from "@/components/ui-kit/SmartSelect"
import { Item } from "@/lib/items"
import { OfferInput, OfferScope, OfferDiscountType, Offer } from "@/lib/offers"

const offerSchema = z
  .object({
    name: z.string().min(1, "Name is required"),
    code: z.string().max(64, "Code too long").optional(),
    discountType: z.enum(["PERCENT", "FLAT"]),
    scope: z.enum(["CART", "ITEM"]),
    discountValue: z.coerce.number().min(0.01, "Discount value must be > 0"),
    itemId: z.string().optional(),
    startDate: z.string().optional(),
    endDate: z.string().optional(),
    active: z.boolean(),
    usageLimit: z.coerce.number().int().positive().optional(),
    minBillAmount: z.coerce.number().min(0, "Minimum bill must be >= 0").optional(),
  })
  .superRefine((v, ctx) => {
    if (v.discountType === "PERCENT" && v.discountValue > 100) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["discountValue"],
        message: "Percent discount must be <= 100",
      })
    }
    if (v.scope === "ITEM" && (!v.itemId || v.itemId.trim() === "")) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["itemId"],
        message: "Item is required for item-specific offers",
      })
    }
    if (v.startDate && v.endDate && v.endDate < v.startDate) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["endDate"],
        message: "End date must be on/after start date",
      })
    }
  })

type FormValues = z.infer<typeof offerSchema>

export function OfferForm({
  initialValue,
  submitLabel,
  onSubmit,
}: {
  initialValue?: Partial<Offer>
  submitLabel: string
  onSubmit: (input: OfferInput) => Promise<void>
}) {
  const router = useRouter()
  const [serverError, setServerError] = useState<string | null>(null)
  const [selectedItemId, setSelectedItemId] = useState<string | undefined>(
    initialValue?.itemId || undefined
  )

  const form = useForm<FormValues>({
    resolver: zodResolver(offerSchema),
    defaultValues: {
      name: initialValue?.name ?? "",
      code: initialValue?.code ?? "",
      discountType: (initialValue?.discountType as OfferDiscountType) ?? "PERCENT",
      scope: (initialValue?.scope as OfferScope) ?? "CART",
      discountValue: initialValue?.discountValue ?? 0,
      itemId: initialValue?.itemId ?? undefined,
      startDate: initialValue?.startDate ?? "",
      endDate: initialValue?.endDate ?? "",
      active: initialValue?.active ?? true,
      usageLimit: initialValue?.usageLimit ?? undefined,
      minBillAmount: initialValue?.minBillAmount ?? undefined,
    },
  })

  const scope = form.watch("scope")

  useEffect(() => {
    if (scope !== "ITEM") {
      setSelectedItemId(undefined)
      form.setValue("itemId", undefined)
    }
  }, [scope, form])

  const [isAdmin, setIsAdmin] = useState(false)

  useEffect(() => {
    try {
      setIsAdmin(window.localStorage.getItem("role") === "ADMIN")
    } catch {
      setIsAdmin(false)
    }
  }, [])

  async function handleSubmit(values: FormValues) {
    if (!isAdmin) return
    setServerError(null)
    try {
      const input: OfferInput = {
        name: values.name.trim(),
        code: (values.code ?? "").trim() ? (values.code ?? "").trim() : null,
        discountType: values.discountType,
        scope: values.scope,
        discountValue: Number(values.discountValue),
        itemId: values.scope === "ITEM" ? (values.itemId ?? null) : null,
        startDate: values.startDate ? values.startDate : null,
        endDate: values.endDate ? values.endDate : null,
        active: values.active,
        usageLimit: values.usageLimit ?? null,
        minBillAmount: values.minBillAmount ?? null,
      }
      await onSubmit(input)
    } catch (err) {
      setServerError(err instanceof Error ? err.message : "An error occurred")
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)}>
        <FormLayout
          stickyFooter={
            <>
              <Button type="button" variant="outline" onClick={() => router.back()}>
                Cancel
              </Button>
              <Button type="submit" disabled={!isAdmin || form.formState.isSubmitting}>
                {form.formState.isSubmitting ? "Saving..." : submitLabel}
              </Button>
            </>
          }
        >
          {serverError ? (
            <div className="bg-destructive/15 text-destructive px-4 py-2 rounded-md">{serverError}</div>
          ) : null}

          {!isAdmin ? (
            <div className="bg-muted text-muted-foreground px-4 py-2 rounded-md text-sm">
              Only admins can create or edit offers.
            </div>
          ) : null}

          <FormSectionCard title="Offer Details">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Name</FormLabel>
                    <FormControl>
                      <Input placeholder="New Year Sale" {...field} />
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
                    <FormLabel>Coupon Code</FormLabel>
                    <FormControl>
                      <Input placeholder="Optional (e.g. NY2026)" {...field} value={field.value || ""} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="discountType"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Discount Type</FormLabel>
                    <FormControl>
                      <select
                        className="w-full h-10 rounded-md border border-input bg-background px-3"
                        value={field.value}
                        onChange={field.onChange}
                      >
                        <option value="PERCENT">Percent (%)</option>
                        <option value="FLAT">Flat amount</option>
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="scope"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Applies To</FormLabel>
                    <FormControl>
                      <select
                        className="w-full h-10 rounded-md border border-input bg-background px-3"
                        value={field.value}
                        onChange={field.onChange}
                      >
                        <option value="CART">Full cart / bill</option>
                        <option value="ITEM">Specific item</option>
                      </select>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="discountValue"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Discount Value</FormLabel>
                    <FormControl>
                      <Input type="number" step="0.01" placeholder="0" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="active"
                render={({ field }) => (
                  <FormItem className="flex items-center gap-3">
                    <FormControl>
                      <input
                        type="checkbox"
                        checked={field.value}
                        onChange={(e) => field.onChange(e.target.checked)}
                      />
                    </FormControl>
                    <FormLabel className="m-0">Active</FormLabel>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
          </FormSectionCard>

          <FormSectionCard title="Conditions">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {scope === "ITEM" ? (
                <FormField
                  control={form.control}
                  name="itemId"
                  render={() => (
                    <FormItem className="md:col-span-2">
                      <FormLabel>Item</FormLabel>
                      <FormControl>
                        <SmartSelect<Item>
                          endpoint="/api/v1/items"
                          labelKey="name"
                          valueKey="id"
                          value={selectedItemId}
                          placeholder="Select item..."
                          onSelect={(id) => {
                            setSelectedItemId(id)
                            form.setValue("itemId", id)
                          }}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              ) : null}

              <FormField
                control={form.control}
                name="startDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Start Date</FormLabel>
                    <FormControl>
                      <Input type="date" {...field} value={field.value || ""} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="endDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>End Date</FormLabel>
                    <FormControl>
                      <Input type="date" {...field} value={field.value || ""} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="usageLimit"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Usage Limit</FormLabel>
                    <FormControl>
                      <Input type="number" step="1" placeholder="Optional" {...field} value={field.value ?? ""} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="minBillAmount"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Minimum Bill Amount</FormLabel>
                    <FormControl>
                      <Input type="number" step="0.01" placeholder="Optional" {...field} value={field.value ?? ""} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
          </FormSectionCard>
        </FormLayout>
      </form>
    </Form>
  )
}
